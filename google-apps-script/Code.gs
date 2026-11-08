/**
 * Google Apps Script Web App for MeloGroove (melody1) Centralized User Storage
 * Stores every user's profile JSON in the owner's Google Drive folder ("MeloGroove_Users_DB")
 * and logs/updates a master row in Google Sheets ("MeloGroove_Users_Sheet").
 *
 * Deployment Instructions:
 * 1. Go to https://script.google.com and create a New Project.
 * 2. Paste this code into Code.gs and click Save.
 * 3. Click "Deploy" -> "New deployment" -> Select type: "Web app".
 * 4. Set "Execute as": "Me (Project Owner)" and "Who has access": "Anyone".
 * 5. Copy the Web App URL (https://script.google.com/macros/s/.../exec) and paste it into MeloGroove!
 */
const FOLDER_NAME = "MeloGroove_Users_DB";
const SHEET_NAME = "MeloGroove_Users_Sheet";

function getOrCreateFolder() {
  const folders = DriveApp.getFoldersByName(FOLDER_NAME);
  return folders.hasNext() ? folders.next() : DriveApp.createFolder(FOLDER_NAME);
}

function getOrCreateSheet(folder) {
  const files = folder.getFilesByName(SHEET_NAME);
  let ss;
  if (files.hasNext()) {
    ss = SpreadsheetApp.open(files.next());
  } else {
    ss = SpreadsheetApp.create(SHEET_NAME);
    DriveApp.getFileById(ss.getId()).moveTo(folder);
    const sheet = ss.getActiveSheet();
    sheet.appendRow([
      "uid", "email", "displayName", "phoneNumber", "phoneVerified",
      "selectedCountry", "favoriteGenres", "bio", "createdAt", "lastLoginAt", "deviceInfo", "photoURL"
    ]);
  }
  return ss.getActiveSheet();
}

function doPost(e) {
  try {
    const body = JSON.parse(e.postData.contents || "{}");
    const action = body.action || "";
    const folder = getOrCreateFolder();
    const sheet = getOrCreateSheet(folder);

    if (action === "get_user") {
      const uid = body.uid || "";
      const email = (body.email || "").toLowerCase();
      const fileName = "user_" + (uid || email) + ".json";
      const files = folder.getFilesByName(fileName);
      if (files.hasNext()) {
        const content = files.next().getBlob().getDataAsString("UTF-8");
        return jsonResponse({ status: "ok", found: true, user: JSON.parse(content) });
      }
      return jsonResponse({ status: "ok", found: false });
    }

    if (action === "upsert_user") {
      const u = body.user || {};
      const uid = u.uid || "";
      const email = (u.email || "").toLowerCase();
      const fileName = "user_" + (uid || email) + ".json";
      const jsonStr = JSON.stringify(u, null, 2);

      // 1. Save or update individual JSON file in Admin Google Drive
      const existing = folder.getFilesByName(fileName);
      if (existing.hasNext()) {
        existing.next().setContent(jsonStr);
      } else {
        folder.createFile(fileName, jsonStr, MimeType.PLAIN_TEXT);
      }

      // 2. Upsert row in Admin Google Sheet
      const data = sheet.getDataRange().getValues();
      let rowIndex = -1;
      for (let i = 1; i < data.length; i++) {
        if (data[i][0] === uid || String(data[i][1]).toLowerCase() === email) {
          rowIndex = i + 1;
          break;
        }
      }
      const rowValues = [
        u.uid || "", u.email || "", u.displayName || "", u.phoneNumber || "",
        Boolean(u.phoneVerified), u.selectedCountry || "",
        (u.favoriteGenres || []).join(", "), u.bio || "",
        u.createdAt || "", u.lastLoginAt || "", u.deviceInfo || "", u.photoURL || ""
      ];
      if (rowIndex > 0) {
        sheet.getRange(rowIndex, 1, 1, rowValues.length).setValues([rowValues]);
      } else {
        sheet.appendRow(rowValues);
      }
      return jsonResponse({ status: "ok", saved: true });
    }

    if (action === "send_otp") {
      const email = body.email || "";
      const otpCode = body.otpCode || "";
      const phone = body.phoneNumber || "";
      if (email && otpCode) {
        try {
          MailApp.sendEmail({
            to: email,
            subject: "کد تایید شماره موبایل MeloGroove: " + otpCode,
            body: "شماره موبایل: " + phone + "\nکد تایید ۶ رقمی شما: " + otpCode
          });
        } catch (mailErr) {}
      }
      return jsonResponse({ status: "ok", dispatched: true });
    }

    return jsonResponse({ status: "error", message: "Unknown action: " + action });
  } catch (err) {
    return jsonResponse({ status: "error", message: String(err) });
  }
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
