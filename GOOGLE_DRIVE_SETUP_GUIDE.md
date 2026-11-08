# راهنمای اتصال رایگان به Google Drive مالک پروژه (`MeloGroove / melody1`)

این پروژه به گونه‌ای طراحی شده که بدون نیاز به هیچ دیتابیس پولی، اطلاعات تمامی کاربران را در **Google Drive اکانت ادمین (مالک پروژه)** هم به صورت فایل اختصاصی `JSON` برای هر کاربر (`user_<uid>.json`) و هم در یک جدول جامع **Google Sheet** (`MeloGroove_Users_Sheet`) ذخیره، بازیابی و بروزرسانی کند و همزمان در حافظه محلی دستگاه کاربر (`UserPreferences` / `localStorage`) کش نماید.

---

## مراحل راه‌اندازی ۱ دقیقه‌ای در Google Drive شما

1. وارد آدرس **[https://script.google.com](https://script.google.com)** با اکانت جیمیل ادمین شوید و روی **New Project (پروژه جدید)** کلیک کنید.
2. محتویات فایل [`google-apps-script/Code.gs`](google-apps-script/Code.gs) (که در داخل خود اپلیکیشن در بخش **پروفایل کاربری** نیز با یک دکمه قابل کپی است) را در ویرایشگر قرار داده و ذخیره (`Ctrl+S`) کنید.
3. در بالای صفحه روی دکمه آبی **Deploy** و سپس **New deployment** کلیک کنید.
4. از چرخ‌دنده کنار `Select type` گزینه **Web app** را انتخاب کنید:
   - **Execute as:** `Me` (اکانت جیمیل خودتان)
   - **Who has access:** `Anyone`
5. روی **Deploy** کلیک کرده و دسترسی به Google Drive و Google Sheets را تایید (`Authorize access`) کنید.
6. لینک **Web App URL** تولید شده (که با `https://script.google.com/macros/s/.../exec` شروع می‌شود) را کپی کرده و در صفحه **پروفایل کاربری** یا **تنظیمات** اپلیکیشن در فیلد `GOOGLE_DRIVE_WEBHOOK_URL` وارد نمایید.

---

## ساختار JSON ذخیره‌شده برای هر کاربر در پوشه `MeloGroove_Users_DB`

```json
{
  "uid": "google_unique_id",
  "email": "user@gmail.com",
  "displayName": "نام کاربر (اولیه از جیمیل - قابل ویرایش)",
  "photoURL": "https://lh3.googleusercontent.com/...",
  "phoneNumber": "+989120000000",
  "phoneVerified": true,
  "selectedCountry": "IR",
  "favoriteGenres": ["Pop", "Traditional"],
  "bio": "توضیحات دلخواه کاربر",
  "createdAt": "2026-11-07T18:00:00.000Z",
  "lastLoginAt": "2026-11-07T18:05:00.000Z",
  "deviceInfo": "Android 15 (SDK 35) - Google Pixel"
}
```
