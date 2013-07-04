Secure-preferences
==================

This is Android Shared preference wrapper that encrypts the keys and values of Shared Preferences using 256-bit AES. *The key is stored in the perferences and so can be read and extracted by root user.* Keys and values are encrypted and base64 encooded before storing into prefs. 

Much of the original code is from Daniel Abraham article on codeproject 
http://www.codeproject.com/Articles/549119/Encryption-Wrapper-for-Android-SharedPreferences This project was created and shared on Github with his permission. 

For the initial import to github I've modified the project structure and included the Android base64 class so library can be used by Android 2.1+. The sample project is pretty basic but shows getting/setting prefs and dumps current prefs to illustrate the fact they are stored encrypted and Base64 encoded. 

###Disclaimer
It's not bullet proof security (in fact it's more like obfuscation of the perferences) but it's a quick win for incrementally making your android app more secure. For instance it'll stop users on rooted devices easily modifiying your app's shared prefs. 

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
