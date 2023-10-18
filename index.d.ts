declare module '@ekreative/react-native-braintree' {
  export interface BraintreeResponse {
    nonce: string;
    deviceData: string;
  }

  export interface BraintreeOptions {
    clientToken: string;
    amount: string;
    currencyCode: string;
  }

  export interface Run3DSecureCheckOptions
    extends Omit<BraintreeOptions, "currencyCode" | "clientToken"> {
    nonce: string;
    /* Pass clientToken if previously no RNBraintree methods were run. */
    clientToken?: string;
    /* Provide as many of the following fields as possible. */
    email?: string;
    firstname?: string;
    lastname?: string;
    phoneNumber?: string;
    streetAddress?: string;
    streetAddress2?: string;
    city?: string;
    region?: string;
    postalCode?: string;
    countryCode?: string;
  }

  export interface TokenizeCardOptions {
    clientToken: string;
    number: string;
    expirationMonth: string;
    expirationYear: string;
    cvv: string;
    postalCode?: string;
  }

  export interface RunApplePayOptions extends BraintreeOptions {
    companyName: string;
  }

  export interface PayPalBillingAgreementOptions {
    clientToken: string;
    description?: string;
    localeCode?: string;
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
    requestPayPalBillingAgreement(options: PayPalBillingAgreementOptions): Promise<BraintreeResponse>;
  }

  const RNBraintree: RNBraintreeModule;

  export default RNBraintree;
}
