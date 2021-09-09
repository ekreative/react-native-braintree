# @ekreative/react-native-braintree

## Getting started

## Android Specific
Add this to your `build.gradle`

```groovy
    repositories {
        maven {
            url "https://cardinalcommerceprod.jfrog.io/artifactory/android"
            credentials {
                username 'braintree_team_sdk'
                password 'AKCp8jQcoDy2hxSWhDAUQKXLDPDx6NYRkqrgFLRc3qDrayg6rrCbJpsKKyMwaykVL8FWusJpp'
            }
        }
    }
```

In Your `AndroidManifest.xml`, `android:allowBackup="false"` can be replaced `android:allowBackup="true"`, it is responsible for app backup.

Also, add this intent-filter to your main activity in `AndroidManifest.xml`

```xml
<activity>
    ...
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="${applicationId}.braintree" />
    </intent-filter>
</activity>

```
**NOTE: Card payments does not work on rooted devices and Android Emulators**

## iOS Specific
```bash
cd ios
pod install
```
###### Configure a new URL scheme
Add a bundle url scheme {BUNDLE_IDENTIFIER}.payments in your app Info via XCode or manually in the Info.plist. In your Info.plist, you should have something like: 

```xml 
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleTypeRole</key>
        <string>Editor</string>
        <key>CFBundleURLName</key>
        <string>com.myapp</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>com.myapp.payments</string>
        </array>
    </dict>
</array>
```
###### Update your code
In your `AppDelegate.m`:

```objective-c
#import "BraintreeCore.h"

...
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    ...
    [BTAppSwitch setReturnURLScheme:self.paymentsURLScheme];
}

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options {

    if ([url.scheme localizedCaseInsensitiveCompare:self.paymentsURLScheme] == NSOrderedSame) {
        return [BTAppSwitch handleOpenURL:url options:options];
    }
    
    return [RCTLinkingManager application:application openURL:url options:options];
}

- (NSString *)paymentsURLScheme {
    NSString *bundleIdentifier = [[NSBundle mainBundle] bundleIdentifier];
    return [NSString stringWithFormat:@"%@.%@", bundleIdentifier, @"payments"];
}
```


## Usage

##### Show PayPall module

```javascript
import RNBraintree from '@ekreative/react-native-braintree';

RNBraintree.showPayPalModule({
    clientToken: 'CLIENT_TOKEN_GENERATED_ON_SERVER_SIDE',
    amount: '1.0',
    currencyCode: 'EUR'
    })
    .then(result => console.log(result))
    .catch((error) => console.log(error));


```

##### Card tokenization
```javascript
import RNBraintree from '@ekreative/react-native-braintree';

RNBraintree.tokenizeCard({
    clientToken: 'CLIENT_TOKEN_GENERATED_ON_SERVER_SIDE',
    number: '1111222233334444',
    expirationMonth: '11',
    expirationYear: '24',
    cvv: '123',
    postalCode: '',
    })
    .then(result => console.log(result))
    .catch((error) => console.log(error));

```
##### Make Payment
```javascript
import RNBraintree from '@ekreative/react-native-braintree';

RNBraintree.run3DSecureCheck({
    clientToken: 'CLIENT_TOKEN_GENERATED_ON_SERVER_SIDE',
    nonce: 'CARD_NONCE',
    amount: '122.00',
    email: 'email@mail.com',
    firstname: '',
    lastname: '',
    phoneNumber: '',
    streetAddress: '',
    streetAddress2: '',
    city: '',
    region: '',
    postalCode: '',
    countryCode: ''
    })
    .then(result => console.log(result))
    .catch((error) => console.log(error));
```

### iOS
##### Get if Apple Pay available
```javascript
import RNBraintree from '@ekreative/react-native-braintree';

console.log(RNBraintree.isApplePayAvailable())
```
##### Make payment using Apple Pay
```javascript
import RNBraintree from '@ekreative/react-native-braintree';

RNBraintree.runApplePay({
    clientToken: 'CLIENT_TOKEN_GENERATED_ON_SERVER_SIDE',
    companyName: 'Company',
    amount: '100.0',
    currencyCode: 'EUR'
    })
    .then(result => console.log(result))
    .catch((error) => console.log(error));
```
### Android
##### Make payment using Google Pay
```javascript
import RNBraintree from '@ekreative/react-native-braintree';

RNBraintree.runGooglePay({
    clientToken: 'CLIENT_TOKEN_GENERATED_ON_SERVER_SIDE',
    amount: '100.0',
    currencyCode: 'EUR'
    })
    .then(result => console.log(result))
    .catch((error) => console.log(error));
```