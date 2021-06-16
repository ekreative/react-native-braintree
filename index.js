// main index.js

import { NativeModules } from 'react-native';

const { RNBraintree } = NativeModules;
const { RNBraintreeApplePay } = NativeModules;

export default {
    showPayPalModule: RNBraintree.showPayPalModule,
    runGooglePay: RNBraintree.runGooglePay,
    run3DSecureCheck: RNBraintree.run3DSecureCheck,
    tokenizeCard: RNBraintree.tokenizeCard,
    runApplePay: RNBraintreeApplePay && RNBraintreeApplePay.runApplePay,
    requestPayPalBillingAgreement: RNBraintree.requestPayPalBillingAgreement,
}
