declare module '@ekreative/react-native-braintree' {
  interface BraintreeResponse {
    nonce: string;
    deviceData: string;
  }

  interface BraintreeOptions {
    clientToken: string;
    amount: string;
    currencyCode: string;
  }

  interface Run3DSecureCheckOptions
    extends Omit<BraintreeOptions, 'currencyCode'> {
    nonce: string;
    email: string;
    firstname: string;
    lastname: string;
    phoneNumber: string;
    streetAddress: string;
    streetAddress2?: string;
    city: string;
    region?: string;
    postalCode: string;
    countryCode: string;
  }

  interface TokenizeCardOptions {
    clientToken: string;
    number: string;
    expirationMonth: string;
    expirationYear: string;
    cvv: string;
    postalCode?: string;
  }

  interface RunApplePayOptions extends BraintreeOptions {
    companyName: string;
  }

  // Export

  interface RNBraintreeModule {
    showPayPalModule(options: BraintreeOptions): Promise<BraintreeResponse>;
    runGooglePay(options: BraintreeOptions): Promise<BraintreeResponse>;
    run3DSecureCheck(
      options: Run3DSecureCheckOptions,
    ): Promise<BraintreeResponse>;
    tokenizeCard(options: TokenizeCardOptions): Promise<BraintreeResponse>;
    runApplePay(options: RunApplePayOptions): Promise<BraintreeResponse>;
  }

  const RNBraintree: RNBraintreeModule;

  export default RNBraintree;
}
