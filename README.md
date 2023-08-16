# sms-backup-android
Android library to Backup and Restore SMS

---
🟩 __Setup:__
  
  ⚡Add it in your root build.gradle at the end of repositories:
  ```gradle
  allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  ```

  ⚡Add the dependency 
  
  *current version:* [![](https://jitpack.io/v/fcat97/sms-backup-android.svg)](https://jitpack.io/#fcat97/sms-backup-android)

  ```gradle
  dependencies {
	        implementation 'com.github.fcat97:sms-backup-android:version'
	}
  ```

🟩 __Usage:__
  
  This package provides two class `SmsReader` and `SmsWriter` and a model class `Sms`.

  - 🟢 Read from inbox: `SmsReader().readAllSmsFromInbox(context)`
  - 🟢 Read from backup file: `SmsReader()..readFromBackup(context, uri)`
  - 🟢 Create a backup: `SmsWriter().createBackup(context, smsList, uri)`
  - 🟢 Restore sms to inbox (Need to be default sms app): `SmsWriter().writeSmsToInbox(context, smsList, onProgress)`
  
