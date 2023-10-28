const mockNativeModules = {
  RNBraintree: {
    showPayPalModule: jest.fn(),
    runGooglePay: jest.fn(),
    run3DSecureCheck: jest.fn(),
    tokenizeCard: jest.fn(),
    getDeviceData: jest.fn(),
  },
  RNBraintreeApplePay: {
    runApplePay: undefined,
  },
};

jest.mock('react-native', () => {
  return {
    NativeModules: mockNativeModules,
  };
});

describe('index.js test', () => {
  it('should check if runApplePay is defined', () => {
    expect(mockNativeModules.RNBraintreeApplePay.runApplePay).not.toBeDefined();
  });
});
