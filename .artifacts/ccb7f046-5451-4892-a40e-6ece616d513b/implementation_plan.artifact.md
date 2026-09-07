# Fix Missing Resources in UDBET

The build is currently failing because several string resources referenced in `activity_login.xml` and `activity_register.xml` are not defined in `strings.xml`. Additionally, some hardcoded strings in `LoginActivity` and `RegisterActivity` should be externalized.

## Proposed Changes

### [Component Name] Resource Management

#### [MODIFY] [strings.xml](file:///C:/Users/kazut/Music/UDBET/app/src/main/res/values/strings.xml)
- Add all missing string resources used in layouts.
- Add strings for Toast messages currently hardcoded in Kotlin files.

#### [MODIFY] [LoginActivity.kt](file:///C:/Users/kazut/Music/UDBET/app/src/main/java/udb/edu/sv/dsm/udbet/LoginActivity.kt)
- Replace hardcoded strings with resource references.

#### [MODIFY] [RegisterActivity.kt](file:///C:/Users/kazut/Music/UDBET/app/src/main/java/udb/edu/sv/dsm/udbet/RegisterActivity.kt)
- Replace hardcoded strings with resource references.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify that the project builds successfully.

### Manual Verification
- Deploy the app to a device or emulator and check if the login and register screens display the correct texts and if Toasts work as expected.
