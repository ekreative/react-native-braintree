# @ekreative/react-native-braintree

## Початок роботи

## Специфічно для Android
Додайте це до свого `build.gradle`

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

У вашому `AndroidManifest.xml`, `android:allowBackup="false"` можна замінити `android:allowBackup="true"`, він відповідає за резервне копіювання програми.

Крім того, додайте наступний intent-filter до основного activity в `AndroidManifest.xml`

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
**ПРИМІТКА: Платежі карткою не працюють на пристроях з рутованим доступом і емуляторах Android**

Якщо ваш проект використовує Proguard, додайте наступні рядки у `proguard-rules.pro` файл
```
-keep class com.cardinalcommerce.dependencies.internal.bouncycastle.**
-keep class com.cardinalcommerce.dependencies.internal.nimbusds.**
-keep class com.cardinalcommerce.shared.**
```

## Специфічно для iOS
```bash
cd ios
pod install
```
###### Налаштуйте нову схему URL-адреси
Додайте схему URL-адреси пакета {BUNDLE_IDENTIFIER}.payments в Info свого додатка через XCode або вручну в Info.plist. У вашому Info.plist ви повинні мати щось на кшталт:

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
###### Оновіть свій код
У вашому `AppDelegate.m`:

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


## Використання

##### Показати модуль PayPal

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

##### Токенізація картки
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
##### Здійснити оплату
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

##### Запит на платіжну угоду PayPal
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
##### Перевірте, чи доступний Apple Pay
```javascript
import RNBraintree from '@ekreative/react-native-braintree';

console.log(RNBraintree.isApplePayAvailable())
```
##### Здійсніть оплату за допомогою Apple Pay
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
##### Здійсніть оплату за допомогою Google Pay
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

## ЗРОБИТИ

- [ ] Android. Перевірте, чи можемо ми розділити логіку в `getDeviceData` методі, щоб викликати `new DataCollector(mBraintreeClient).collectDeviceData()` лише один раз (схоже, що зараз його можна викликати вдруге з `setup` методу) https://github.com/ekreative/react-native-braintree/pull/37#issuecomment-1752470507
- [ ] iOS. Спробуйте використати новий`getDeviceData` метод в інших методах, наприклад `tokenizeCard`, `showPayPalModule` https://github.com/ekreative/react-native-braintree/pull/37#issuecomment-1752470507
