Secure-preferences
==================

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-secure--preferences-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/362)

This is Android Shared preference wrapper that encrypts the keys and values of Shared Preferences using 256-bit AES. **The key is stored in the perferences and so can be read and extracted by root user.** Keys and values are encrypted and base64 encooded before storing into prefs. 

The sample app is availbile on [playstore](https://play.google.com/store/apps/details?id=com.securepreferences.sample)

Much of the original code is from Daniel Abraham article on [codeproject](http://www.codeproject.com/Articles/549119/Encryption-Wrapper-for-Android-SharedPreferences). This project was created and shared on Github with his permission. 

![screenshot](https://raw.github.com/scottyab/secure-preferences/master/docs/images/ss_frame_secure_pref.png "Sample app Screenshot")
 

##Dependency
```java
	dependencies {
    	compile 'com.scottyab:secure-preferences-lib:0.0.4'
	}
```

##Release Notes:
0.0.5 (coming soon)
* default AES to CBC mode for increased security

0.0.4
* Gralde support thanks @yelinaung 
* Fix for OnPreferenceChanged listener @richardleggett 

0.0.3

* Added test Project
* Updated sample ready for playstore upload 

0.0.2

* Added methods to get/set strings un-encrypted 
* Added backup PBKDF function in case PBKDF2WithHmacSHA1 not supported
* Refactored code to make it easier to change the AES mode and PBKDF function. 
* Increased iterations of PBKDF from 1000 to 2000. 

0.0.1 

* Initial import to github I've modified the project structure. 
* Included the Android base64 class so library can be used by Android 2.1+. 
* Enhanced the sample project dumps current prefs to illustrate the fact they are stored encrypted and Base64 encoded. 


###Disclaimer
It's not bullet proof security (in fact it's more like obfuscation of the perferences) but it's a quick win for incrementally making your android app more secure. For instance it'll stop users on rooted devices easily modifiying your app's shared prefs. 


###Contributing 
Please do send me pull requests, but also bugs and enhancement requests are welcome. Although no guarantees on when I can review them.  


###Licence 
Apache License, Version 2.0



    Copyright (C) 2013, Daniel Abraham, Scott Alexander-Bown

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
