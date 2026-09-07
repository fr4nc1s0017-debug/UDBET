# Walkthrough - Fixing Missing Resources and Refactoring Strings

I have fixed the build error by adding the missing string resources and refactored the activities to use these resources instead of hardcoded strings.

## Changes Made

### Resources
- Added 14 new string resources to [strings.xml](file:///C:/Users/kazut/Music/UDBET/app/src/main/res/values/strings.xml) covering titles, hints, buttons, and messages for both Login and Register screens.

### Activities
- Refactored [LoginActivity.kt](file:///C:/Users/kazut/Music/UDBET/app/src/main/java/udb/edu/sv/dsm/udbet/LoginActivity.kt) to use `getString(R.string.welcome_msg)` and `getString(R.string.error_empty_fields)` for its Toast messages.
- Refactored [RegisterActivity.kt](file:///C:/Users/kazut/Music/UDBET/app/src/main/java/udb/edu/sv/dsm/udbet/RegisterActivity.kt) to use `getString(R.string.register_success)`, `getString(R.string.error_password_mismatch)`, and `getString(R.string.error_empty_fields)` for its Toast messages.

## Verification Results

### Automated Tests
- Ran `:app:assembleDebug` and the build finished successfully.

> [!TIP]
> Using `strings.xml` instead of hardcoded strings is a best practice that makes your app easier to translate (localize) and maintain in the future.
