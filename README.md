# webview

Android Studio project and python script that generates an application APK that opens a WebView with a specific site according to:

- `-n`/`--name`: Name of application (i.e. what is seen as name in the Launcher)
- `-p`/`--package`: Unique package name, suggest `com.webapp.example`
- `-u`/`--url`: Start URL for the application

The application can access geolocation and has a very rudimentary password manager (currently implemented as encrypted shared storage, consider security implications before using).

Example usage: `python ./generate.py -n "GitHub" -p "com.webapp.github" -u "https://github.com"`  
If one or more switches are missing those will be requested interactively
