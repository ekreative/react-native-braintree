// RNBraintreeModule.java

package com.ekreative.reactnativebraintree;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import com.braintreepayments.api.BraintreeClient;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.CardClient;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.GooglePayClient;
import com.braintreepayments.api.GooglePayRequest;
import com.braintreepayments.api.PayPalCheckoutRequest;
import com.braintreepayments.api.PayPalClient;
import com.braintreepayments.api.PayPalPaymentIntent;
import com.braintreepayments.api.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.ThreeDSecureClient;
import com.braintreepayments.api.ThreeDSecurePostalAddress;
import com.braintreepayments.api.ThreeDSecureRequest;
import com.braintreepayments.api.ThreeDSecureResult;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

public class RNBraintreeModule extends ReactContextBaseJavaModule {

    private static final int GOOGLE_PAYMENT_REQUEST_CODE = 79129;
    private final Context mContext;
    private final FragmentActivity mCurrentActivity;
    private Promise mPromise;
    private String mDeviceData;
    private String mToken;
    private BraintreeClient mBraintreeClient;
    private PayPalClient mPayPalClient;
    private GooglePayClient mGooglePayClient;
    private CardClient mCardClient;
    private ThreeDSecureClient mThreeDSecureClient;

    public RNBraintreeModule(ReactApplicationContext reactContext) {
        super(reactContext);

        mContext = reactContext;
        mCurrentActivity = (FragmentActivity) getCurrentActivity();

        //todo: handle paypal and google pay result
    }

    @Override
    public String getName() {
        return "RNBraintree";
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
                PayPalCheckoutRequest request = new PayPalCheckoutRequest(parameters.getString("amount"));
                request.setCurrencyCode(currency);
                request.setIntent(PayPalPaymentIntent.AUTHORIZE);
                mPayPalClient.tokenizePayPalAccount(
                        mCurrentActivity,
                        request,
                        e -> promise.reject(e.getMessage()));
            }
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
                        e -> promise.reject(e.getMessage()));
            }
        }
    }

    @ReactMethod
    public void tokenizeCard(final ReadableMap parameters, final Promise promise) {
        mPromise = promise;

        if (!parameters.hasKey("clientToken")) {
            promise.reject("You must provide a clientToken");
        } else {
            setup(parameters.getString("clientToken"));

            mCardClient = new CardClient(mBraintreeClient);

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

            mCardClient.tokenize(card, (cardNonce, exception) -> {
                if (exception != null) {
                    mPromise.reject(exception.getMessage());
                    return;
                }
                if (cardNonce != null) {
                    WritableMap result = Arguments.createMap();
                    result.putString("nonce", cardNonce.getString());
                    sendResult(result);
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
                    (threeDSecureResult, e) -> {
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

    private void handleThreeDSecureResult(ThreeDSecureResult threeDSecureResult, Exception error){
        if(threeDSecureResult != null && threeDSecureResult.getTokenizedCard() != null){
            WritableMap result = Arguments.createMap();
            result.putString("nonce", threeDSecureResult.getTokenizedCard().getString());
            sendResult(result);
        }
    }


    public void setup(final String token) {
        if (mBraintreeClient == null || !token.equals(mToken)) {
            mBraintreeClient = new BraintreeClient(mContext, token);
        }
        new DataCollector(mBraintreeClient).collectDeviceData(
                mContext,
                (result, e) -> mDeviceData = result);
        mToken = token;
    }

    public void sendResult(final WritableMap result) {
        result.putString("deviceData", mDeviceData);
        mPromise.resolve(result);
    }
}
