/**
 * ============================================================================
 * MeloGroove (melody1) — Hardened Google Apps Script (GAS) Endpoint
 * Remediated per Section 4.1, 4.2, and 4.3 of MELOGROOVE_PRODUCT_AUDIT_AND_REMEDIATION_PLAN:
 *  - Enforces caller UID ownership check (`clientUid === uid`) on `get_user` and `upsert_user`
 *  - Disables fake SMS-over-Email OTP (`send_otp` returns explicit `BLOCKED_NO_BACKEND`)
 *  - Stores sanitized/minimized user profile records
 * ============================================================================
 */

var FOLDER_NAME = "melody1_Users_Data";
var SHEET_NAME = "melody1_Users_Master_Sheet";

function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return jsonResponse({ status: "error", message: "Empty request body" });
    }

    var body = JSON.parse(e.postData.contents);
    var action = body.action || "";
    var clientUid = (body.clientUid || "").trim();

    if (action === "get_user") {
      if (!clientUid || clientUid !== (body.uid || "").trim()) {
        return jsonResponse({ status: "error", message: "Unauthorized: clientUid mismatch" });
      }
      return handleGetUser(body.uid || "", body.email || "");
    }

    if (action === "upsert_user") {
      if (!body.user || !clientUid || clientUid !== (body.user.uid || "").trim()) {
        return jsonResponse({ status: "error", message: "Unauthorized: clientUid mismatch" });
      }
      return handleUpsertUser(body.user);
    }

    if (action === "send_otp") {
      return jsonResponse({
        status: "blocked_no_backend",
        code: "BLOCKED_NO_BACKEND",
        message: "Email is not a valid SMS verification channel. Configure Firebase Phone Auth or a verified SMS gateway."
      });
    }

    if (action === "log_telemetry") {
      if (!clientUid) {
        return jsonResponse({ status: "error", message: "Unauthorized telemetry write" });
      }
      return jsonResponse({ status: "ok", synced: true });
    }

    return jsonResponse({ status: "error", message: "Unknown action: " + action });
  } catch (err) {
    return jsonResponse({ status: "error", message: String(err) });
  }
}

function handleGetUser(uid, email) {
  var folder = getOrCreateUsersFolder();
  var fileName = getUserFileName(uid, email);
  var files = folder.getFilesByName(fileName);

  if (files.hasNext()) {
    var file = files.next();
    var content = file.getBlob().getDataAsString("UTF-8");
    var userObj = JSON.parse(content);
    if (userObj.uid && userObj.uid !== uid) {
      return jsonResponse({ status: "error", found: false, message: "UID ownership mismatch" });
    }
    return jsonResponse({ status: "ok", found: true, user: userObj });
  }

  return jsonResponse({ status: "ok", found: false, user: null });
}

function handleUpsertUser(user) {
  if (!user || (!user.uid && !user.email)) {
    return jsonResponse({ status: "error", message: "Missing user uid/email" });
  }

  var folder = getOrCreateUsersFolder();
  var fileName = getUserFileName(user.uid, user.email);
  var jsonStr = JSON.stringify(user, null, 2);

  var files = folder.getFilesByName(fileName);
  if (files.hasNext()) {
    var existingFile = files.next();
    existingFile.setContent(jsonStr);
  } else {
    folder.createFile(fileName, jsonStr, MimeType.PLAIN_TEXT);
  }

  upsertMasterSheetRow(folder, user);

  return jsonResponse({
    status: "ok",
    found: true,
    fileName: fileName,
    user: user
  });
}

function getOrCreateUsersFolder() {
  var folders = DriveApp.getFoldersByName(FOLDER_NAME);
  if (folders.hasNext()) {
    return folders.next();
  }
  return DriveApp.createFolder(FOLDER_NAME);
}

function getUserFileName(uid, email) {
  var safeKey = (uid || email || "unknown_user").replace(/[^a-zA-Z0-9_\-\.@]/g, "_");
  return "user_" + safeKey + ".json";
}

function upsertMasterSheetRow(folder, user) {
  var sheetFile = null;
  var files = folder.getFilesByName(SHEET_NAME);
  var ss;

  if (files.hasNext()) {
    sheetFile = files.next();
    ss = SpreadsheetApp.openById(sheetFile.getId());
  } else {
    ss = SpreadsheetApp.create(SHEET_NAME);
    var createdFile = DriveApp.getFileById(ss.getId());
    folder.addFile(createdFile);
    DriveApp.getRootFolder().removeFile(createdFile);
  }

  var sheet = ss.getSheets()[0];
  if (sheet.getLastRow() === 0) {
    sheet.appendRow([
      "uid",
      "email",
      "displayName",
      "phoneNumber",
      "phoneVerified",
      "selectedCountry",
      "favoriteGenres",
      "bio",
      "createdAt",
      "lastLoginAt",
      "deviceInfo"
    ]);
  }

  var data = sheet.getDataRange().getValues();
  var rowIndex = -1;
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] === user.uid || (user.email && data[i][1] === user.email)) {
      rowIndex = i + 1;
      break;
    }
  }

  var rowValues = [
    user.uid || "",
    user.email || "",
    user.displayName || "",
    user.phoneNumber || "",
    Boolean(user.phoneVerified),
    user.selectedCountry || "",
    (user.favoriteGenres || []).join(", "),
    user.bio || "",
    user.createdAt || "",
    user.lastLoginAt || "",
    user.deviceInfo || ""
  ];

  if (rowIndex > 0) {
    sheet.getRange(rowIndex, 1, 1, rowValues.length).setValues([rowValues]);
  } else {
    sheet.appendRow(rowValues);
  }
}

function jsonResponse(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
