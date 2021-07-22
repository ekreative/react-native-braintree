// RNBraintreeModule.java

package com.ekreative.reactnativebraintree;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.Card;
import com.braintreepayments.api.DataCollector;
import com.braintreepayments.api.GooglePayment;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.ThreeDSecure;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.interfaces.ThreeDSecureLookupListener;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.models.ThreeDSecureLookup;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import static android.app.Activity.RESULT_OK;
import static android.app.Activity.RESULT_CANCELED;

public class RNBraintreeModule extends ReactContextBaseJavaModule {

    private Promise mPromise;
    private BraintreeFragment mBraintreeFragment;
    private String mDeviceData;
    private String mToken;
    private static final int GOOGLE_PAYMENT_REQUEST_CODE = 79129;

    public RNBraintreeModule(ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addActivityEventListener(new BaseActivityEventListener() {
            @Override
            public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
                super.onActivityResult(activity, requestCode, resultCode, data);
                if (requestCode == GOOGLE_PAYMENT_REQUEST_CODE && resultCode == RESULT_OK) {
                    GooglePayment.tokenize(mBraintreeFragment, PaymentData.getFromIntent(data));
                }

                if (requestCode == GOOGLE_PAYMENT_REQUEST_CODE && resultCode == RESULT_CANCELED) {
                    mPromise.reject("USER_CANCELLATION", "The user cancelled");
                }

                // FIXME: PaymentMethodNonceCreatedListener doesn't call when using ThreeDSecureRequest.VERSION_2
                // SOURCE: https://github.com/braintree/braintree_android/issues/268#issuecomment-567268780

                final int unmaskedRequestCode = requestCode & 0x0000ffff;
                boolean isBraintreeFragment =  mBraintreeFragment != null && mBraintreeFragment.getTag().indexOf("Braintree") != -1;

                if (isBraintreeFragment) {
                    mBraintreeFragment.onActivityResult(unmaskedRequestCode, resultCode, data);
                }
            }
        });
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

            try {
                PayPalRequest request = new PayPalRequest(parameters.getString("amount"))
                        .currencyCode(currency)
                        .intent(PayPalRequest.INTENT_AUTHORIZE);
                PayPal.requestOneTimePayment(mBraintreeFragment, request);
            } catch (Exception e) {
                promise.reject(e.getMessage());
            }
        }
    }

    @ReactMethod
    public void requestPayPalBillingAgreement(final ReadableMap parameters, final Promise promise) {
        mPromise = promise;

        if (!parameters.hasKey("clientToken")) {
            promise.reject("MISSING_CLIENT_TOKEN", "You must provide a clientToken");
        }

        setup(parameters.getString("clientToken"));

        try {
            String description = parameters.hasKey("description") ? parameters.getString("description") : "";
            String localeCode = parameters.hasKey("localeCode") ? parameters.getString("localeCode") : "US";

            PayPalRequest request = new PayPalRequest()
                    .localeCode(localeCode)
                    .billingAgreementDescription(description);

            PayPal.requestBillingAgreement(mBraintreeFragment, request);
        } catch (Exception e) {
            promise.reject("REQUEST_BILLING_AGREEMENT_FAILED", e.getMessage());
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

            try {
                GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                        .transactionInfo(TransactionInfo.newBuilder()
                                .setCurrencyCode(currency)
                                .setTotalPrice(parameters.getString("amount"))
                                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                .build())
                        .billingAddressRequired(true);

                if (parameters.hasKey("merchantId")) {
                    String merchantId = parameters.getString("merchantId");
                    googlePaymentRequest.googleMerchantId(merchantId);
                    String env = "test".equals(merchantId) ? "TEST" : "PRODUCTION";
                    googlePaymentRequest.environment(env);
                }

                GooglePayment.requestPayment(mBraintreeFragment, googlePaymentRequest);
            } catch (Exception e) {
                promise.reject(e.getMessage());
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

            try {
                CardBuilder cardBuilder = new CardBuilder().validate(false);

                if (parameters.hasKey("number")) {
                    cardBuilder.cardNumber(parameters.getString("number"));
                }

                if (parameters.hasKey("expirationMonth")) {
                    cardBuilder.expirationMonth(parameters.getString("expirationMonth"));
                }

                if (parameters.hasKey("expirationYear")) {
                    cardBuilder.expirationYear(parameters.getString("expirationYear"));
                }

                if (parameters.hasKey("cvv")) {
                    cardBuilder.cvv(parameters.getString("cvv"));
                }

                if (parameters.hasKey("postalCode")) {
                    cardBuilder.postalCode(parameters.getString("postalCode"));
                }

                Card.tokenize(mBraintreeFragment, cardBuilder);
            } catch (Exception e) {
                promise.reject(e.getMessage());
            }
        }
    }

    @ReactMethod
    public void run3DSecureCheck(final ReadableMap parameters, final Promise promise) {
        mPromise = promise;

        if (!parameters.hasKey("clientToken")) {
            promise.reject("You must provide a clientToken");
        }

        try {
            setup(parameters.getString("clientToken"));

            ThreeDSecurePostalAddress address = new ThreeDSecurePostalAddress();

            if (parameters.hasKey("firstname")) {
                address.givenName(parameters.getString("firstname"));
            }

            if (parameters.hasKey("lastname")) {
                address.surname(parameters.getString("lastname"));
            }

            if (parameters.hasKey("phoneNumber")) {
                address.phoneNumber(parameters.getString("phoneNumber"));
            }

            if (parameters.hasKey("countryCode")) {
                address.countryCodeAlpha2(parameters.getString("countryCode"));
            }

            if (parameters.hasKey("city")) {
                address.locality(parameters.getString("city"));
            }

            if (parameters.hasKey("postalCode")) {
                address.postalCode(parameters.getString("postalCode"));
            }

            if (parameters.hasKey("region")) {
                address.region(parameters.getString("region"));
            }

            if (parameters.hasKey("streetAddress")) {
                address.streetAddress(parameters.getString("streetAddress"));
            }

            if (parameters.hasKey("streetAddress2")) {
                address.extendedAddress(parameters.getString("streetAddress2"));
            }

            // For best results, provide as many additional elements as possible.
            ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                    .shippingAddress(address);

            ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest()
                    .nonce(parameters.getString("nonce"))
                    .email(parameters.getString("email"))
                    .billingAddress(address)
                    .versionRequested(ThreeDSecureRequest.VERSION_2)
                    .additionalInformation(additionalInformation)
                    .amount(parameters.getString("amount"));

            ThreeDSecure.performVerification(mBraintreeFragment, threeDSecureRequest, new ThreeDSecureLookupListener() {
                @Override
                public void onLookupComplete(ThreeDSecureRequest request, ThreeDSecureLookup lookup) {
                    // Optionally inspect the lookup result and prepare UI if a challenge is required
                    ThreeDSecure.continuePerformVerification(mBraintreeFragment, request, lookup);
                }
            });
        } catch (Exception e) {
            promise.reject(e.getMessage());
        }
    }

    public void setup(final String token) {
        if (mBraintreeFragment == null || !token.equals(mToken)) {
            try {
                mBraintreeFragment = BraintreeFragment.newInstance((AppCompatActivity) getCurrentActivity(), token);
                mBraintreeFragment.addListener(new BraintreeCancelListener() {
                    @Override
                    public void onCancel(int requestCode) {
                        mPromise.reject("USER_CANCELLATION", "The user cancelled");
                    }
                });
                mBraintreeFragment.addListener(new PaymentMethodNonceCreatedListener() {
                    @Override
                    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {

                        WritableMap map = Arguments.createMap();
                        if (paymentMethodNonce instanceof PayPalAccountNonce) {
                            PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce) paymentMethodNonce;

                            // Access additional information
                            String email = payPalAccountNonce.getEmail();
                            map.putString("email", email);

                        }
                        map.putString("nonce", paymentMethodNonce.getNonce());
                        sendResult(map);
                    }
                });
                DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
                    @Override
                    public void onResponse(String deviceData) {
                        mDeviceData = deviceData;
                    }
                });
            } catch (Exception e) {
                mPromise.reject(e.getMessage());
            }
        }
        mToken = token;
    }

    public void sendResult(final WritableMap result) {
        result.putString("deviceData", mDeviceData);
        mPromise.resolve(result);
    }
}
