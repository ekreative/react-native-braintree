#import "RNBraintree.h"
#import "BraintreeCore.h"
#import "BTCardClient.h"
#import "BraintreePayPal.h"
#import "BTDataCollector.h"
#import "BraintreePaymentFlow.h"
#import "BraintreeThreeDSecure.h"

@interface RNBraintree() <BTViewControllerPresentingDelegate, BTThreeDSecureRequestDelegate>
@property (nonatomic, strong) BTAPIClient *apiClient;
@property (nonatomic, strong) BTDataCollector *dataCollector;
@end

@implementation RNBraintree

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(showPayPalModule: (NSDictionary *)options
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject) {
    NSString *clientToken = options[@"clientToken"];
    NSString *amount = options[@"amount"];
    NSString *currencyCode = options[@"currencyCode"];

    self.apiClient = [[BTAPIClient alloc] initWithAuthorization: clientToken];
    self.dataCollector = [[BTDataCollector alloc] initWithAPIClient:self.apiClient];
    BTPayPalDriver *payPalDriver = [[BTPayPalDriver alloc] initWithAPIClient: self.apiClient];

    BTPayPalCheckoutRequest *request= [[BTPayPalCheckoutRequest alloc] initWithAmount:amount];
    request.currencyCode = currencyCode;
    [payPalDriver tokenizePayPalAccountWithPayPalRequest:request completion:^(BTPayPalAccountNonce * _Nullable tokenizedPayPalAccount, NSError * _Nullable error) {
        if (error) {
            reject(@"ONE_TIME_PAYMENT_FAILED", error.localizedDescription, nil);
            return;
        }
        if (!tokenizedPayPalAccount) {
            reject(@"ONE_TIME_PAYMENT_CANCELLED", @"Payment has been cancelled", nil);
            return;
        }
        [self.dataCollector collectDeviceData:^(NSString * _Nonnull deviceData) {
            resolve(@{@"deviceData": deviceData,
                      @"email": tokenizedPayPalAccount.email,
                      @"nonce": tokenizedPayPalAccount.nonce,});
        }];
    }];
}

RCT_EXPORT_METHOD(requestPayPalBillingAgreement: (NSDictionary *)options
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject) {
    NSString *clientToken = options[@"clientToken"];
    NSString *description = options[@"description"];
    NSString *userAction = options[@"userAction"];

    self.apiClient = [[BTAPIClient alloc] initWithAuthorization: clientToken];
    self.dataCollector = [[BTDataCollector alloc] initWithAPIClient:self.apiClient];
    BTPayPalDriver *payPalDriver = [[BTPayPalDriver alloc] initWithAPIClient: self.apiClient];

    BTPayPalVaultRequest *request= [[BTPayPalVaultRequest alloc] init];
    if (description) {
        request.billingAgreementDescription = description;
    }
    if (userAction && [@"commit" isEqualToString:userAction]) {
        request.userAction = BTPayPalRequestUserActionCommit;
    }
    [payPalDriver tokenizePayPalAccountWithPayPalRequest:request completion:^(BTPayPalAccountNonce * _Nullable tokenizedPayPalAccount, NSError * _Nullable error) {
        if (error) {
            reject(@"REQUEST_BILLING_AGREEMENT_FAILED", error.localizedDescription, nil);
            return;
        }
        if (!tokenizedPayPalAccount) {
            reject(@"REQUEST_BILLING_AGREEMENT_CANCELLED", @"Request billing agreement has been cancelled", nil);
            return;
        }
        [self.dataCollector collectDeviceData:^(NSString * _Nonnull deviceData) {
            resolve(@{@"deviceData": deviceData,
                      @"nonce": tokenizedPayPalAccount.nonce,});
        }];
    }];
}

RCT_EXPORT_METHOD(tokenizeCard: (NSDictionary *)parameters
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject) {

    NSString *clientToken = parameters[@"clientToken"];
    self.apiClient = [[BTAPIClient alloc] initWithAuthorization: clientToken];
    self.dataCollector = [[BTDataCollector alloc] initWithAPIClient:self.apiClient];
    BTCardClient *cardClient = [[BTCardClient alloc] initWithAPIClient: self.apiClient];

    BTCard *card = [[BTCard alloc] init];
    card.number = parameters[@"number"];
    card.expirationMonth = parameters[@"expirationMonth"];
    card.expirationYear = parameters[@"expirationYear"];
    card.cvv = parameters[@"cvv"];
    card.postalCode = parameters[@"postalCode"];
    card.shouldValidate = NO;
    
    [cardClient tokenizeCard:card completion:^(BTCardNonce * _Nullable tokenizedCard, NSError * _Nullable error) {
        if (error) {
            reject(@"TOKENIZE_FAILED", error.localizedDescription, nil);
            return;
        }
        [self.dataCollector collectDeviceData:^(NSString * _Nonnull deviceData) {
            resolve(@{@"deviceData": deviceData,
                      @"nonce": tokenizedCard.nonce,});
        }];
    }];
}

RCT_EXPORT_METHOD(run3DSecureCheck: (NSDictionary *)parameters
                     resolver: (RCTPromiseResolveBlock)resolve
                     rejecter: (RCTPromiseRejectBlock)reject) {
    [self startPaymentFlow:parameters
                  resolver:resolve
                  rejecter:reject];
}

RCT_EXPORT_METHOD(getDeviceData: (NSString *) clientToken
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject) {
    self.apiClient = [[BTAPIClient alloc] initWithAuthorization: clientToken];
    self.dataCollector = [[BTDataCollector alloc] initWithAPIClient:self.apiClient];
    [self.dataCollector collectDeviceData:^(NSString * _Nonnull deviceData) {
        resolve(deviceData);
    }];
}

#pragma mark - 3D Secure
- (void)startPaymentFlow: (NSDictionary *)parameters
                resolver: (RCTPromiseResolveBlock)resolve
                rejecter: (RCTPromiseRejectBlock)reject {
    NSString *clientToken = parameters[@"clientToken"];
    if (self.apiClient == NULL) {
        if (clientToken == NULL) {
            reject(@"MISSING_CLIENT_TOKEN", @"clientToken must be passed if Braintree methods weren't run before", nil);
        }
        self.apiClient = [[BTAPIClient alloc] initWithAuthorization: clientToken];
    }
    self.dataCollector = [[BTDataCollector alloc] initWithAPIClient:self.apiClient];

    BTThreeDSecureRequest *threeDSecureRequest = [[BTThreeDSecureRequest alloc] init];
    threeDSecureRequest.amount = [NSDecimalNumber decimalNumberWithString: parameters[@"amount"]];
    threeDSecureRequest.nonce =  parameters[@"nonce"];
    threeDSecureRequest.threeDSecureRequestDelegate = self;
    threeDSecureRequest.email = parameters[@"email"];
    threeDSecureRequest.versionRequested = BTThreeDSecureVersion2;

    BTThreeDSecurePostalAddress *address = [BTThreeDSecurePostalAddress new];
    address.givenName =  parameters[@"firstname"]; // ASCII-printable characters required, else will throw a validation error
    address.surname = parameters[@"lastname"]; // ASCII-printable characters required, else will throw a validation error
    address.phoneNumber = parameters[@"phoneNumber"];
    address.streetAddress = parameters[@"streetAddress"];
    address.extendedAddress = parameters[@"streetAddress2"];
    address.locality = parameters[@"city"];
    address.region = parameters[@"region"];
    address.postalCode = parameters[@"postalCode"];
    address.countryCodeAlpha2 = parameters[@"countryCode"];

    BTThreeDSecureAdditionalInformation *additionalInformation = [BTThreeDSecureAdditionalInformation new];
    additionalInformation.shippingAddress = address;

    threeDSecureRequest.additionalInformation = additionalInformation;
    threeDSecureRequest.billingAddress = address;

    BTPaymentFlowDriver *paymentFlowDriver = [[BTPaymentFlowDriver alloc] initWithAPIClient:self.apiClient];
    paymentFlowDriver.viewControllerPresentingDelegate = self;

    [paymentFlowDriver startPaymentFlow:threeDSecureRequest completion:^(BTPaymentFlowResult * _Nonnull result, NSError * _Nonnull error) {
        if (error) {
            reject(@"PAYMENT_FAILED", error.localizedDescription, nil);
            return;
        }
        BTThreeDSecureResult *threeDSecureResult = (BTThreeDSecureResult *)result;
        if (!threeDSecureResult.tokenizedCard.threeDSecureInfo.liabilityShiftPossible && threeDSecureResult.tokenizedCard.threeDSecureInfo.wasVerified) {
            reject(@"3DSECURE_NOT_ABLE_TO_SHIFT_LIABILITY", @"3D Secure liability cannot be shifted", nil);
            return;
        }
        if (!threeDSecureResult.tokenizedCard.threeDSecureInfo.liabilityShifted && threeDSecureResult.tokenizedCard.threeDSecureInfo.wasVerified) {
            reject(@"3DSECURE_LIABILITY_NOT_SHIFTED", @"3D Secure liability was not shifted", nil);
        }
        if (!threeDSecureResult.tokenizedCard.nonce) {
            reject(@"PAYMENT_3D_SECURE_FAILED", @"Something went wrong", nil);
            return;
        }
        [self.dataCollector collectDeviceData:^(NSString * _Nonnull deviceData) {
            resolve(@{@"deviceData": deviceData,
                      @"nonce": threeDSecureResult.tokenizedCard.nonce});
        }];
        return;
    }];
}

#pragma mark - BTViewControllerPresentingDelegate
- (void)paymentDriver:(nonnull id)driver requestsPresentationOfViewController:(nonnull UIViewController *)viewController {
    [self.reactRoot presentViewController:viewController animated:YES completion:nil];
}

- (void)paymentDriver:(nonnull id)driver requestsDismissalOfViewController:(nonnull UIViewController *)viewController {
    [viewController dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark - BTThreeDSecureRequestDelegate

- (void)onLookupComplete:(BTThreeDSecureRequest *)request lookupResult:(BTThreeDSecureResult *)result next:(void (^)(void))next {
    next();
}

#pragma mark - RootController
- (UIViewController*)reactRoot {
    UIViewController *topViewController  = [UIApplication sharedApplication].keyWindow.rootViewController;
    if (topViewController.presentedViewController) {
        topViewController = topViewController.presentedViewController;
    }
    return topViewController;
}

@end
