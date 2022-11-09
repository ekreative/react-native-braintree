@import PassKit;
#import "RNBraintreeApplePay.h"
#import "BraintreeCore.h"
#import "BTDataCollector.h"
#import "BraintreePaymentFlow.h"
#import "BraintreeApplePay.h"

@interface RNBraintreeApplePay()<PKPaymentAuthorizationViewControllerDelegate>

@property (nonatomic, strong) BTAPIClient *apiClient;
@property (nonatomic, strong) BTDataCollector *dataCollector;
@property (nonatomic, strong) RCTPromiseResolveBlock resolve;
@property (nonatomic, strong) RCTPromiseRejectBlock reject;
@property (nonatomic, assign) BOOL isApplePaymentAuthorized;

@end

@implementation RNBraintreeApplePay

RCT_EXPORT_MODULE()

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(isApplePayAvailable) {
    BOOL canMakePayments = [PKPaymentAuthorizationViewController canMakePaymentsUsingNetworks:
            @[PKPaymentNetworkVisa, PKPaymentNetworkMasterCard, PKPaymentNetworkAmex, PKPaymentNetworkDiscover]];
    return [NSNumber numberWithBool:canMakePayments];
}

RCT_EXPORT_METHOD(runApplePay: (NSDictionary *)options
                     resolver: (RCTPromiseResolveBlock)resolve
                     rejecter: (RCTPromiseRejectBlock)reject) {
    NSString *companyName = options[@"companyName"];
    NSString *amount = options[@"amount"];
    NSString *clientToken = options[@"clientToken"];
    NSString *currencyCode = options[@"currencyCode"];
    if (!companyName) {
        reject(@"NO_COMPANY_NAME", @"You must provide a `companyName`", nil);
        return;
    }
    if (!amount) {
        reject(@"NO_TOTAL_PRICE", @"You must provide a `amount`", nil);
        return;
    }

    self.apiClient = [[BTAPIClient alloc] initWithAuthorization: clientToken];
    self.dataCollector = [[BTDataCollector alloc] initWithAPIClient:self.apiClient];

    BTApplePayClient *applePayClient = [[BTApplePayClient alloc] initWithAPIClient: self.apiClient];

    [applePayClient paymentRequest:^(PKPaymentRequest * _Nullable paymentRequest, NSError * _Nullable error) {
        if (error) {
            reject(@"APPLE_PAY_PAYMENT_REQUEST_FAILED", error.localizedDescription, nil);
            return;
        }

        if (@available(iOS 11.0, *)) {
            paymentRequest.requiredBillingContactFields = [NSSet setWithObject:PKContactFieldPostalAddress];
        }
        paymentRequest.merchantCapabilities = PKMerchantCapability3DS;
        paymentRequest.paymentSummaryItems = @[
            [PKPaymentSummaryItem summaryItemWithLabel:companyName amount:[NSDecimalNumber decimalNumberWithString:amount]]
        ];
        paymentRequest.currencyCode = currencyCode;
        self.resolve = resolve;
        self.reject = reject;
        [self setIsApplePaymentAuthorized:NO];
        PKPaymentAuthorizationViewController *paymentController = [[PKPaymentAuthorizationViewController alloc] initWithPaymentRequest:paymentRequest];
        paymentController.delegate = self;
        [[self reactRoot] presentViewController:paymentController animated:YES completion:NULL];
    }];
}

- (void)handleTokenizationResult: (BTApplePayCardNonce *)tokenizedApplePayPayment
                           error: (NSError *)error
                      completion: (void (^)(PKPaymentAuthorizationStatus))completion{
    if (!tokenizedApplePayPayment && self.reject) {
        self.reject(error.localizedDescription, error.localizedDescription, error);
        completion(PKPaymentAuthorizationStatusFailure);
        [self resetPaymentResolvers];
        return;
    }
    [self.dataCollector collectDeviceData:^(NSString * _Nonnull deviceData) {
        if (self.resolve) {
            self.resolve(@{@"deviceData": deviceData,
                        @"nonce": tokenizedApplePayPayment.nonce});
            completion(PKPaymentAuthorizationStatusSuccess);
            [self resetPaymentResolvers];
        }
    }];
}

- (void)resetPaymentResolvers {
    self.resolve = NULL;
    self.reject = NULL;
}

#pragma mark - PKPaymentAuthorizationViewControllerDelegate
- (void)paymentAuthorizationViewController:(PKPaymentAuthorizationViewController *)controller
                       didAuthorizePayment:(PKPayment *)payment
                                completion:(void (^)(PKPaymentAuthorizationStatus))completion {
    [self setIsApplePaymentAuthorized: YES];
    BTApplePayClient *applePayClient = [[BTApplePayClient alloc] initWithAPIClient:self.apiClient];
    [applePayClient tokenizeApplePayPayment:payment
                                 completion:^(BTApplePayCardNonce *tokenizedApplePayPayment, NSError *error) {
        [self handleTokenizationResult:tokenizedApplePayPayment error:error completion:completion];
    }];
}

- (void)paymentAuthorizationViewControllerDidFinish:(nonnull PKPaymentAuthorizationViewController *)controller {
    [controller dismissViewControllerAnimated:YES completion:NULL];
    if (self.reject && [self isApplePaymentAuthorized]) {
        self.reject(@"APPLE_PAY_FAILED", @"Apple Pay failed", nil);
    }
    if (self.isApplePaymentAuthorized == NO) {
        self.reject(@"USER_CANCELLATION", @"The user cancelled", nil);
    }
    [self resetPaymentResolvers];
    self.isApplePaymentAuthorized = NULL;
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
