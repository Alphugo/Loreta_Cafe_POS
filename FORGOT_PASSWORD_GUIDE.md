# Forgot Password Flow - Complete Guide

## ✅ What Has Been Fixed

The forgot password functionality has been completely fixed and now works for both Firebase and Local Authentication users.

## 🔄 How It Works

### For Firebase Users:
1. User enters email
2. System checks if email exists in Firebase
3. Firebase sends password reset email directly to user's inbox
4. User clicks link in email to reset password
5. Done! (No OTP code needed)

### For Local Auth Users:
1. User enters email
2. System checks if email exists in local database
3. System generates 6-digit OTP code
4. Code is displayed in Logcat and Toast (for testing)
5. User enters 6-digit code
6. User sets new password
7. Done!

## 📱 Step-by-Step Testing

### Test with Local Auth (Works Immediately)

1. **Open the app**
2. **Click "Forgot Password"** on login screen
3. **Enter email:** `temp@loreta.com` (or any email that exists in local database)
4. **Click "Continue"**
5. **Check the result:**
   - You'll see a Toast message: "6-digit code sent to [email]"
   - The code will be shown in the Toast (for testing)
   - **Also check Logcat** - the code is logged there too
6. **You'll be taken to OTP Verification screen**
7. **Enter the 6-digit code** you saw in the Toast/Logcat
8. **Click "Continue"**
9. **Enter new password** (minimum 8 characters)
10. **Confirm password**
11. **Click "Confirm"**
12. **Success!** You'll see the success screen

### Test with Firebase (Requires Firebase Setup)

1. **Make sure Firebase is set up** (see FIREBASE_SETUP_GUIDE.md)
2. **Create a user in Firebase Console** → Authentication
3. **Open the app**
4. **Click "Forgot Password"**
5. **Enter the Firebase user's email**
6. **Click "Continue"**
7. **Check your email inbox** for password reset link
8. **Click the link** in the email
9. **Set new password**
10. **Done!**

## 🔍 Finding the OTP Code (For Local Auth Testing)

### Method 1: Check Toast Message
- When you request password reset, a Toast appears showing the code
- Example: "6-digit code sent to temp@loreta.com\nCode: 123456 (for testing)"

### Method 2: Check Logcat
1. Open Android Studio
2. Go to **Logcat** tab
3. Filter by: `PasswordResetService` or `ResetPasswordActivity`
4. Look for logs like:
```
═══════════════════════════════════════
PASSWORD RESET CODE
Email: temp@loreta.com
6-Digit Code: 123456
Code expires in 5 minutes
═══════════════════════════════════════
```

### Method 3: Check Console Output
- The code is also printed to console/logs
- Look for "OTP Code for [email]: [code]"

## ⚠️ Important Notes

### For Testing (Local Auth):
- **The OTP code is shown in Toast and Logcat** for easy testing
- **Code expires in 5 minutes**
- **Code can only be used once**

### For Production:
- **Remove the code from Toast** - it's only for testing!
- **Integrate real email service** (see below)
- **Hide code from logs** in production builds

## 🔧 Email Integration (For Production)

Currently, the email sending is mocked for testing. To send real emails, integrate one of these:

### Option 1: Firebase Cloud Functions + SendGrid
```javascript
// Cloud Function
exports.sendPasswordResetEmail = functions.https.onCall(async (data, context) => {
  const { email, code } = data;
  // Send email via SendGrid
  // ...
});
```

### Option 2: Backend API
- Create an API endpoint
- Use SMTP or email service
- Call from Android app

### Option 3: Third-party Service
- SendGrid
- Mailgun
- AWS SES
- Firebase Extensions

## 🐛 Troubleshooting

### Issue: "Email not found"
**Solution:**
- Make sure the email exists in the database
- For local auth: Use `temp@loreta.com`
- For Firebase: Create user in Firebase Console first

### Issue: "Invalid or expired code"
**Solution:**
- Code expires in 5 minutes
- Code can only be used once
- Request a new code

### Issue: "Can't see OTP code"
**Solution:**
- Check Toast message (shown briefly)
- Check Logcat in Android Studio
- Filter by "PasswordResetService"

### Issue: Firebase password reset not working
**Solution:**
- Make sure Firebase is set up correctly
- Check `google-services.json` is in place
- Verify Email/Password provider is enabled
- Check email inbox (including spam folder)

## ✅ Complete Flow Diagram

```
User clicks "Forgot Password"
         ↓
Enter email address
         ↓
System checks email:
    ├─ Firebase Auth? → Send Firebase reset email → User clicks link → Reset password
    └─ Local Auth? → Generate 6-digit code → Show code → User enters code → Set new password
```

## 📝 Code Locations

- **ResetPasswordActivity.java** - Email entry and initial check
- **OtpVerificationActivity.java** - 6-digit code verification
- **NewPasswordActivity.java** - New password entry
- **PasswordResetService.java** - Code generation and verification
- **FirebaseAuthRepository.java** - Firebase password reset

## 🎯 Testing Checklist

- [ ] Can enter email in forgot password screen
- [ ] Valid email format is accepted
- [ ] Invalid email shows error
- [ ] Existing email (local) generates OTP code
- [ ] OTP code is visible in Toast/Logcat
- [ ] Can navigate to OTP verification screen
- [ ] Can enter 6-digit code
- [ ] Invalid code shows error
- [ ] Valid code proceeds to new password screen
- [ ] Can set new password (min 8 characters)
- [ ] Password confirmation works
- [ ] Successfully resets password
- [ ] Can login with new password

---

**The forgot password flow is now fully functional!** 🎉

Test it with `temp@loreta.com` to see the complete OTP flow in action.

