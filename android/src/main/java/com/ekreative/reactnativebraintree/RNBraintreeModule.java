// RNBraintreeModule.java

package com.ekreative.reactnativebraintree;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.BraintreeRequestCodes;
import com.braintreepayments.api.BrowserSwitchResult;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.GooglePayClient;
import com.braintreepayments.api.GooglePayRequest;
import com.braintreepayments.api.PayPalAccountNonce;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.braintreepayments.api.PayPalVaultRequest;
import com.braintreepayments.api.PaymentMethodNonce;
import com.braintreepayments.api.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.ThreeDSecureClient;
import com.braintreepayments.api.ThreeDSecurePostalAddress;
import com.braintreepayments.api.ThreeDSecureRequest;
import com.braintreepayments.api.ThreeDSecureResult;
import com.braintreepayments.api.UserCanceledException;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

public class RNBraintreeModule extends ReactContextBaseJavaModule
        implements ActivityEventListener, LifecycleEventListener {

    private final Context mContext;
    private FragmentActivity mCurrentActivity;
    private Promise mPromise;
    private String mDeviceData;
    private String mToken;
    private BraintreeClient mBraintreeClient;
    private PayPalClient mPayPalClient;
    private GooglePayClient mGooglePayClient;
    private ThreeDSecureClient mThreeDSecureClient;

    @Override
    public String getName() {
        return "RNBraintree";
    }

    public RNBraintreeModule(ReactApplicationContext reactContext) {
        super(reactContext);

        mContext = reactContext;

        reactContext.addLifecycleEventListener(this);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case BraintreeRequestCodes.GOOGLE_PAY:
                if (mGooglePayClient != null) {
                    mGooglePayClient.onActivityResult(
                            resultCode,
                            intent,
                            this::handleGooglePayResult
                    );
                }
                break;
            case BraintreeRequestCodes.THREE_D_SECURE:
                if (mThreeDSecureClient != null) {
                    mThreeDSecureClient.onActivityResult(
                            resultCode,
                            intent,
                            this::handleThreeDSecureResult
                    );
                }
                break;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (mCurrentActivity != null) {
            mCurrentActivity.setIntent(intent);
        }
    }

    @Override
    public void onHostResume() {
        if (mBraintreeClient != null && mCurrentActivity != null) {
            BrowserSwitchResult browserSwitchResult =
                    mBraintreeClient.deliverBrowserSwitchResult(mCurrentActivity);
            if (browserSwitchResult != null) {
                switch (browserSwitchResult.getRequestCode()) {
                    case BraintreeRequestCodes.PAYPAL:
                        if (mPayPalClient != null) {
                            mPayPalClient.onBrowserSwitchResult(
                                    browserSwitchResult,
                                    this::handlePayPalResult
                            );
                        }
                        break;
                    case BraintreeRequestCodes.THREE_D_SECURE:
                        if (mThreeDSecureClient != null) {
                            mThreeDSecureClient.onBrowserSwitchResult(
                                    browserSwitchResult,
                                    this::handleThreeDSecureResult
                            );
                        }
                        break;
                }
            }
        }
    }

    @ReactMethod
    public void showPayPalModule(final ReadableMap parameters, final Promise promise) {
        mPromise = promise;

        if (!parameters.hasKey("clientToken")) {
            promise.reject("You must provide a clientToken");
        } else {
            setup(parameters.getString("clientToken"));

            String currency = "USD";
            if (!parameters.hasKey("amount")) {
                promise.reject("You must provide a amount");
            }
            if (parameters.hasKey("currencyCode")) {
                currency = parameters.getString("currencyCode");
            }
            if (mCurrentActivity != null) {
                mPayPalClient = new PayPalClient(mBraintreeClient);
                PayPalCheckoutRequest request = new PayPalCheckoutRequest(
                        parameters.getString("amount")
                );
                request.setCurrencyCode(currency);
                request.setIntent(PayPalPaymentIntent.AUTHORIZE);
                mPayPalClient.tokenizePayPalAccount(
                        mCurrentActivity,
                        request,
                        e -> handlePayPalResult(null, e));
            }
        }
    }

    @ReactMethod
    public void requestPayPalBillingAgreement(
            final ReadableMap parameters,
            final Promise promise
    ) {
        mPromise = promise;

        if (!parameters.hasKey("clientToken")) {
            promise.reject("MISSING_CLIENT_TOKEN", "You must provide a clientToken");
        }

        setup(parameters.getString("clientToken"));

        String description = parameters.hasKey("description") ?
                parameters.getString("description") :
                "";
        String localeCode = parameters.hasKey("localeCode") ?
                parameters.getString("localeCode") :
                "US";

        if (mCurrentActivity != null) {
            mPayPalClient = new PayPalClient(mBraintreeClient);
            PayPalVaultRequest request = new PayPalVaultRequest();
            request.setLocaleCode(localeCode);
            request.setBillingAgreementDescription(description);

            mPayPalClient.tokenizePayPalAccount(
                    mCurrentActivity,
                    request,
                    e -> handlePayPalResult(null, e));
        }

    }

    private void handlePayPalResult(
            @Nullable PayPalAccountNonce payPalAccountNonce,
            @Nullable Exception error
    ) {
        if (error != null) {
            handleError(error);
            return;
        }
        if (payPalAccountNonce != null) {
            sendPaymentMethodNonceResult(payPalAccountNonce.getString());
        }
    }

    @ReactMethod
    public void runGooglePay(final ReadableMap parameters, final Promise promise) {
        mPromise = promise;

        if (!parameters.hasKey("clientToken")) {
            promise.reject("You must provide a clientToken");
        } else {
            setup(parameters.getString("clientToken"));

            String currency = "USD";
            if (!parameters.hasKey("amount")) {
                promise.reject("You must provide a amount");
            }
            if (parameters.hasKey("currencyCode")) {
                currency = parameters.getString("currencyCode");
            }
            if (mCurrentActivity != null) {
                mGooglePayClient = new GooglePayClient(mBraintreeClient);

                GooglePayRequest googlePayRequest = new GooglePayRequest();
                googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                        .setCurrencyCode(currency)
                        .setTotalPrice(parameters.getString("amount"))
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .build());
                googlePayRequest.setBillingAddressRequired(true);

                if (parameters.hasKey("merchantId")) {
                    String merchantId = parameters.getString("merchantId");
                    googlePayRequest.setGoogleMerchantId(merchantId);
                    String env = "test".equals(merchantId) ? "TEST" : "PRODUCTION";
                    googlePayRequest.setEnvironment(env);
                }

                mGooglePayClient.requestPayment(
                        mCurrentActivity,
                        googlePayRequest,
                        e -> handleGooglePayResult(null, e));
            }
        }
    }

    private void handleGooglePayResult(PaymentMethodNonce nonce, Exception error) {
        if (error != null) {
            handleError(error);
            return;
        }
        if (nonce != null) {
            sendPaymentMethodNonceResult(nonce.getString());
        }
    }

    @ReactMethod
    public void tokenizeCard(final ReadableMap parameters, final Promise promise) {
        mPromise = promise;

        if (!parameters.hasKey("clientToken")) {
            promise.reject("You must provide a clientToken");
        } else {
            setup(parameters.getString("clientToken"));

            CardClient cardClient = new CardClient(mBraintreeClient);

            Card card = new Card();

            if (parameters.hasKey("number")) {
                card.setNumber(parameters.getString("number"));
            }

            if (parameters.hasKey("expirationMonth")) {
                card.setExpirationMonth(parameters.getString("expirationMonth"));
            }

            if (parameters.hasKey("expirationYear")) {
                card.setExpirationYear(parameters.getString("expirationYear"));
            }

            if (parameters.hasKey("cvv")) {
                card.setCvv(parameters.getString("cvv"));
            }

            if (parameters.hasKey("postalCode")) {
                card.setPostalCode(parameters.getString("postalCode"));
            }

            cardClient.tokenize(card, (cardNonce, error) -> {
                if (error != null) {
                    handleError(error);
                    return;
                }
                if (cardNonce != null) {
                    sendPaymentMethodNonceResult(cardNonce.getString());
                }
            });
        }
    }

    @ReactMethod
    public void run3DSecureCheck(final ReadableMap parameters, final Promise promise) {
        mPromise = promise;

        if (!parameters.hasKey("clientToken")) {
            promise.reject("You must provide a clientToken");
        }

        setup(parameters.getString("clientToken"));

        ThreeDSecurePostalAddress address = new ThreeDSecurePostalAddress();

        if (parameters.hasKey("firstname")) {
            address.setGivenName(parameters.getString("firstname"));
        }

        if (parameters.hasKey("lastname")) {
            address.setSurname(parameters.getString("lastname"));
        }

        if (parameters.hasKey("phoneNumber")) {
            address.setPhoneNumber(parameters.getString("phoneNumber"));
        }

        if (parameters.hasKey("countryCode")) {
            address.setCountryCodeAlpha2(parameters.getString("countryCode"));
        }

        if (parameters.hasKey("city")) {
            address.setLocality(parameters.getString("city"));
        }

        if (parameters.hasKey("postalCode")) {
            address.setPostalCode(parameters.getString("postalCode"));
        }

        if (parameters.hasKey("region")) {
            address.setRegion(parameters.getString("region"));
        }

        if (parameters.hasKey("streetAddress")) {
            address.setStreetAddress(parameters.getString("streetAddress"));
        }

        if (parameters.hasKey("streetAddress2")) {
            address.setExtendedAddress(parameters.getString("streetAddress2"));
        }

        if (mCurrentActivity != null) {
            mThreeDSecureClient = new ThreeDSecureClient(mBraintreeClient);

            // For best results, provide as many additional elements as possible.
            ThreeDSecureAdditionalInformation additionalInformation =
                    new ThreeDSecureAdditionalInformation();
            additionalInformation.setShippingAddress(address);

            final ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest();
            threeDSecureRequest.setNonce(parameters.getString("nonce"));
            threeDSecureRequest.setEmail(parameters.getString("email"));
            threeDSecureRequest.setBillingAddress(address);
            threeDSecureRequest.setVersionRequested(ThreeDSecureRequest.VERSION_2);
            threeDSecureRequest.setAdditionalInformation(additionalInformation);
            threeDSecureRequest.setAmount(parameters.getString("amount"));

            mThreeDSecureClient.performVerification(
                    mCurrentActivity,
                    threeDSecureRequest,
                    (threeDSecureResult, error) -> {
                if (error != null) {
                    handleError(error);
                    return;
                }
                if (threeDSecureResult != null) {
                    mThreeDSecureClient.continuePerformVerification(
                                    mCurrentActivity,
                                    threeDSecureRequest,
                                    threeDSecureResult,
                                    this::handleThreeDSecureResult);
                }
            });
        }
    }

    @ReactMethod
    public void getDeviceData(final String clientToken, final Promise promise) {
        setup(clientToken);
        new DataCollector(mBraintreeClient).collectDeviceData(
                mContext,
                (result, e) -> promise.resolve(result));
    }


    private void handleThreeDSecureResult(ThreeDSecureResult threeDSecureResult, Exception error) {
        if (error != null) {
            handleError(error);
            return;
        }
        if (threeDSecureResult != null && threeDSecureResult.getTokenizedCard() != null) {
            sendPaymentMethodNonceResult(threeDSecureResult.getTokenizedCard().getString());
        }
    }


    private void setup(final String token) {
        if (mBraintreeClient == null || !token.equals(mToken)) {
            mCurrentActivity = (FragmentActivity) getCurrentActivity();
            mBraintreeClient = new BraintreeClient(mContext, token);

            new DataCollector(mBraintreeClient).collectDeviceData(
                    mContext,
                    (result, e) -> mDeviceData = result);
            mToken = token;

        }
    }

    private void handleError(Exception error) {
        if (mPromise != null) {
            if (error instanceof UserCanceledException) {
                mPromise.reject("USER_CANCELLATION", "The user cancelled");
            }
            mPromise.reject(error.getMessage());
        }
    }

    private void sendPaymentMethodNonceResult(String nonce) {
        if (mPromise != null) {
            WritableMap result = Arguments.createMap();
            result.putString("deviceData", mDeviceData);
            result.putString("nonce", nonce);
            mPromise.resolve(result);
        }
    }

    @Override
    public void onHostPause() {
        //NOTE: empty implementation
    }

    @Override
    public void onHostDestroy() {
        //NOTE: empty implementation
    }
}
