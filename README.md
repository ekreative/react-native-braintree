
<div align="center">

| Published Version | Package Version | Android SDK | iOS SDK |
|:-----------:|:---------------:|:-----------:|:-------:|
|[![npm](https://img.shields.io/npm/v/react-native-braintree.svg)](https://www.npmjs.com/package/react-native-braintree) | [v2.4.0](https://github.com/ekreative/react-native-braintree/releases/tag/v2.4.0) | 28 | 12.0 |

</div>


# @ekreative/react-native-braintree

## Getting started

## Android Specific
Add this to your `build.gradle`

```groovy
allprojects {
    repositories {
        maven {
            // Braintree 3D Secure
            // https://developer.paypal.com/braintree/docs/guides/3d-secure/client-side/android/v4#generate-a-client-token
            url "https://cardinalcommerceprod.jfrog.io/artifactory/android"
            credentials {
                username 'braintree_team_sdk'
                password 'AKCp8jQcoDy2hxSWhDAUQKXLDPDx6NYRkqrgFLRc3qDrayg6rrCbJpsKKyMwaykVL8FWusJpp'
            }
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
**NOTE: Card payments do not work on rooted devices and Android Emulators**

If your project uses Progurad, add the following lines into `proguard-rules.pro` file
```
-keep class com.cardinalcommerce.dependencies.internal.bouncycastle.**
-keep class com.cardinalcommerce.dependencies.internal.nimbusds.**
-keep class com.cardinalcommerce.shared.**
```

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
    [BTAppContextSwitcher setReturnURLScheme:self.paymentsURLScheme];
}

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options {

    if ([url.scheme localizedCaseInsensitiveCompare:self.paymentsURLScheme] == NSOrderedSame) {
        return [BTAppContextSwitcher handleOpenURL:url];
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
    // Optional if you ran `tokenizeCard()` or other Braintree methods before
    clientToken: 'CLIENT_TOKEN_GENERATED_ON_SERVER_SIDE',
    nonce: 'CARD_NONCE',
    amount: '122.00',
    // Pass as many of the following fields as possible, but they're optional
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

##### Request PayPal billing agreement
```javascript
import RNBraintree from '@ekreative/react-native-braintree';

RNBraintree.requestPayPalBillingAgreement({
    clientToken: 'CLIENT_TOKEN_GENERATED_ON_SERVER_SIDE',
    description: 'BILLING_AGRREEMENT_DESCRIPTION',
    localeCode: 'LOCALE_CODE'
    })
    .then(result => console.log(result))
    .catch((error) => console.log(error));
```
### iOS
##### Check if Apple Pay is available
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

## TODO

- [ ] Android. Check if we can split the logic in the `getDeviceData` method to call `new DataCollector(mBraintreeClient).collectDeviceData()` only once (it seems like it's currently may be called a second time from the `setup` method) https://github.com/ekreative/react-native-braintree/pull/37#issuecomment-1752470507
- [ ] iOS. Try to use the new `getDeviceData` method in other methods, such as `tokenizeCard`, `showPayPalModule` https://github.com/ekreative/react-native-braintree/pull/37#issuecomment-1752470507

## Useful Links and Resources
If you want to read further you can follow these links

- https://reactnative.directory/?search=react-native-braintree
- https://reintech.io/blog/tutorial-for-react-developers-what-are-the-best-react-native-libraries-for-creating-mobile-payments
- https://reactnativeexample.com/a-react-native-interface-for-integrating-payments-using-braintree/
- https://www.npmjs.com/package/@ekreative/react-native-braintree


## Contributors

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->
<!-- ALL-CONTRIBUTORS-LIST:END -->
