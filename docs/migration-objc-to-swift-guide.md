# Objective-C to Swift Codebase Migration Guide

## Introduction
This document serves as a guide to migrate an existing codebase written in Objective-C to Swift. Migrating to Swift offers several benefits, including improved safety, readability, and maintainability of your code. This guide will provide an overview of the migration process and best practices to follow.

**Note:** This guide assumes that you have a basic understanding of both Objective-C and Swift.

## Table of Contents
1. [Preparation](#preparation)
2. [Xcode Project Configuration](#xcode-project-configuration)
3. [Migration Steps](#migration-steps)
    - [RNBraintree] (#RNBraintree)
    - [RNBraintreeApplePay]
4. [Common Migration Challenges](#common-migration-challenges)
5. [Resources](#resources)

## 1. Preparation <a name="preparation"></a>

Before you start the migration, it's important to make sure you have the following in place:

- **Back Up Your Code:** Create a backup of your Objective-C codebase to ensure you can return to it if needed.

- **Version Control:** Ensure that your codebase is under version control (e.g., Git). This will help track changes and collaborate effectively.

- **Latest Xcode Version:** Make sure you are using the latest version of Xcode with support for both Objective-C and Swift.

## 2. Xcode Project Configuration <a name="xcode-project-configuration"></a>

- Open your Xcode project.

- Set your project's build settings to use Swift as the primary language.

- Configure your project settings to use a Bridging Header for Objective-C to Swift interoperability.

## 3. Migration Steps <a name="migration-steps"></a>

### RNBrainTree <a name="RNBraintree"></a>

Create new Swift file with the name `RNBrainTree.swift` and paste the migrated code:
```swift
//  RNBrainTree.swift
import Foundation
import React
import Braintree
import BraintreePayPal
import BraintreeCard 

@objc(RNBraintree)
class RNBraintree: NSObject, BTViewControllerPresentingDelegate, BTThreeDSecureRequestDelegate {
    private var apiClient: BTAPIClient?
    private var dataCollector: BTDataCollector?
    
    @objc
    func showPayPalModule(
        _ options: NSDictionary,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        guard let clientToken = options["clientToken"] as? String,
              let amount = options["amount"] as? String,
              let currencyCode = options["currencyCode"] as? String
        else {
            reject("INVALID_PARAMETERS", "Invalid parameters", nil)
            return
        }
        
        // You may want to check if apiClient and dataCollector are already initialized.
        if self.apiClient == nil {
            self.apiClient = BTAPIClient(authorization: clientToken)
            self.dataCollector = BTDataCollector(apiClient: self.apiClient!)
        }
        
        let payPalDriver = BTPayPalDriver(apiClient: self.apiClient!)
        let request = BTPayPalCheckoutRequest(amount: amount)
        request.currencyCode = currencyCode

        payPalDriver.tokenizePayPalAccount(with: request) { tokenizedPayPalAccount, error in
            if let error = error {
                reject("ONE_TIME_PAYMENT_FAILED", error.localizedDescription, nil)
                return
            }
            
            if tokenizedPayPalAccount == nil {
                reject("ONE_TIME_PAYMENT_CANCELLED", "Payment has been cancelled", nil)
                return
            }

            self.dataCollector.collectDeviceData { deviceData in
                resolve(["deviceData": deviceData, "email": tokenizedPayPalAccount.email, "nonce": tokenizedPayPalAccount?.nonce])
            }
        }
    }

    @objc
    func requestPayPalBillingAgreement(
        _ options: NSDictionary,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {

        guard let clientToken = options["clientToken"] as? String,
              let description = options["description"] as? String
        else {
            reject("INVALID_PARAMETERS", "Invalid parameters", nil)
            return
        }

        // You may want to check if apiClient and dataCollector are already initialized.
        if self.apiClient == nil {
            self.apiClient = BTAPIClient(authorization: clientToken)
            self.dataCollector = BTDataCollector(apiClient: self.apiClient!)
        }
        
        let payPalDriver = BTPayPalDriver(apiClient: self.apiClient!)
        let request = BTPayPalVaultRequest()

        if description {
            request.billingAgreementDescription = description;
        }

        payPalDriver.tokenizePayPalAccount(with: request) { tokenizedPayPalAccount, error in
            if let error = error {
                reject("REQUEST_BILLING_AGREEMENT_FAILED", error.localizedDescription, nil)
                return
            }
            
            if tokenizedPayPalAccount == nil {
                reject("REQUEST_BILLING_AGREEMENT_CANCELLED", "Request billing agreement has been cancelled", nil)
                return
            }

            self.dataCollector.collectDeviceData { deviceData in
                resolve(["deviceData": deviceData, "nonce": tokenizedPayPalAccount.nonce])
            }
        }
    }

    @objc
    func tokenizeCard(
        _ parameters: NSDictionary,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        guard let clientToken = parameters["clientToken"] as? String,
            let number = parameters["number"] as? String,
            let expirationMonth = parameters["expirationMonth"] as? String,
            let expirationYear = parameters["expirationYear"] as? String,
            let cvv = parameters["cvv"] as? String
        else {
            reject("INVALID_PARAMETERS", "Invalid parameters", nil)
            return
        }

        // Initialize the Braintree API client and data collector
        self.apiClient = BTAPIClient(authorization: clientToken)
        self.dataCollector = BTDataCollector(apiClient: self.apiClient!)

        // Initialize the card client
        let cardClient = BTCardClient(apiClient: self.apiClient)

        // Create a BTCard instance and set its properties
        let card = BTCard()
        card.number = number
        card.expirationMonth = expirationMonth
        card.expirationYear = expirationYear
        card.cvv = cvv
        card.postalCode = parameters["postalCode"] as? String
        card.shouldValidate = false

        cardClient.tokenizeCard(card) { tokenizedCard, error in
            if let error = error || tokenizedCard == nil {
                reject("TOKENIZE_FAILED", error.localizedDescription, nil)
                return
            }

            self.dataCollector.collectDeviceData { deviceData in
                resolve(["deviceData": deviceData, "nonce": tokenizedCard.nonce])
            }
        }
    }

    @objc
    func run3DSecureCheck(
        _ parameters: NSDictionary,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        self.startPaymentFlow(parameters, resolver: resolve, rejecter: reject)
    }

    @objc
    func getDeviceData(
        _ clientToken: String,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        if self.apiClient == nil {
            self.apiClient = BTAPIClient(authorization: clientToken)
            self.dataCollector = BTDataCollector(apiClient: self.apiClient!)
        }

        self.dataCollector.collectDeviceData { deviceData in
            resolve(deviceData)
        }
    }

    // MARK:  - 3D Secure
    private func startPaymentFlow(
        _ parameters: NSDictionary,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        guard let clientToken = parameters["clientToken"] as? String
        else {
            reject("MISSING_CLIENT_TOKEN", "clientToken must be passed if Braintree methods weren't run before", nil)
        }
        
        if self.apiClient == nil {
            self.apiClient = BTAPIClient(authorization: clientToken)
            self.dataCollector = BTDataCollector(apiClient: self.apiClient!)
        }

        let threeDSecureRequest = BTThreeDSecureRequest()
        threeDSecureRequest.amount = NSDecimalNumber(string: parameters["amount"] as? String ?? "")
        threeDSecureRequest.nonce = parameters["nonce"] as? String
        threeDSecureRequest.threeDSecureRequestDelegate = self
        threeDSecureRequest.email = parameters["email"] as? String
        threeDSecureRequest.versionRequested = .version2
        
        let address = BTThreeDSecurePostalAddress()
        address.givenName = parameters["firstname"] as? String // ASCII-printable characters required, else will throw a validation error
        address.surname = parameters["lastname"] as? String // ASCII-printable characters required, else will throw a validation error
        address.phoneNumber = parameters["phoneNumber"] as? String
        address.streetAddress = parameters["streetAddress"] as? String
        address.extendedAddress = parameters["streetAddress2"] as? String
        address.locality = parameters["city"] as? String
        address.region = parameters["region"] as? String
        address.postalCode = parameters["postalCode"] as? String
        address.countryCodeAlpha2 = parameters["countryCode"] as? String
        
        let additionalInformation = BTThreeDSecureAdditionalInformation()
        additionalInformation.shippingAddress = address

        threeDSecureRequest.additionalInformation = additionalInformation
        threeDSecureRequest.billingAddress = address
        
        let paymentFlowDriver = BTPaymentFlowDriver(apiClient: apiClient)
        paymentFlowDriver.viewControllerPresentingDelegate = self
        
        paymentFlowDriver.startPaymentFlow(with: threeDSecureRequest) { result, error in
            if let error = error {
                reject("PAYMENT_FAILED", error.localizedDescription, nil)
                return
            }
            
            if let threeDSecureResult = result as? BTThreeDSecureResult,
               let tokenizedCard = threeDSecureResult.tokenizedCard,
               let nonce = tokenizedCard.nonce {
                if !tokenizedCard.threeDSecureInfo.liabilityShiftPossible && tokenizedCard.threeDSecureInfo.wasVerified {
                    reject("3DSECURE_NOT_ABLE_TO_SHIFT_LIABILITY", "3D Secure liability cannot be shifted", nil)
                    return
                }
                if !tokenizedCard.threeDSecureInfo.liabilityShifted && tokenizedCard.threeDSecureInfo.wasVerified {
                    reject("3DSECURE_LIABILITY_NOT_SHIFTED", "3D Secure liability was not shifted", nil)
                }
                resolve(["deviceData": deviceData, "nonce": nonce])
            } else {
                reject("PAYMENT_3D_SECURE_FAILED", "Something went wrong", nil)
            }
        }
    }

    // MARK: - BTViewControllerPresentingDelegate
    func paymentDriver(_ driver: Any, requestsPresentationOf viewController: UIViewController) {
        reactRoot.present(viewController, animated: true, completion: nil)
    }

    func paymentDriver(_ driver: Any, requestsDismissalOf viewController: UIViewController) {
        viewController.dismiss(animated: true, completion: nil)
    }

    // MARK: - BTThreeDSecureRequestDelegate
    func onLookupComplete(_ request: BTThreeDSecureRequest, lookupResult result: BTThreeDSecureResult, next: @escaping () -> Void) {
        next()
    }

    // MARK: - RootController
    var reactRoot: UIViewController? {
        var topViewController = UIApplication.shared.keyWindow?.rootViewController
        if let presentedViewController = topViewController?.presentedViewController {
            topViewController = presentedViewController
        }
        return topViewController
    }
}
```

Replace the content of `RNBraintree.m` with the following
```objective-c
// RNBraintree.m

#import <React/RCTBridgeModule.h>

// Export a native module
@interface RCT_EXTERN_MODULE(RNBraintree, NSObject)

// Export methods to a native module
RCT_EXTERN_METHOD(showPayPalModule: (NSDictionary *)options
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(requestPayPalBillingAgreement: (NSDictionary *)options
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(tokenizeCard: (NSDictionary *)parameters
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(run3DSecureCheck: (NSDictionary *)parameters
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(getDeviceData: (NSString *) clientToken
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject)
@end
```


### RNBrainTreeApplePay
- TODO

## 4. Common Migration Challenges <a name="common-migration-challenges"></a>

- TODO

## 5. Resources <a name="resources"></a>

- https://reactnative.dev/docs/native-modules-ios#exporting-swift
- https://gist.github.com/JofArnold/31dfa8edcc3b8a42bbd86fbd44dad804
- https://gaitatzis.medium.com/react-native-native-modules-in-ios-swift-97eb9073f5a2
