Secure-preferences
==================

Android Shared preference wrapper than encrypts the keys and values of Shared Preferences. The key's can stil be extracted by a determined attacker but it's a quick win for incrementally making your android app more secure. 


Much of the original code is from Daniel Abraham article on codeproject http://www.codeproject.com/Articles/549119/Encryption-Wrapper-for-Android-SharedPreferences. For the initial import to github I've modified the project structure and included the Android base64 class so library can be used by Android 2.1+ 
