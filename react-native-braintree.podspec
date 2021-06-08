# react-native-braintree.podspec

require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-braintree"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                  react-native-braintree
                   DESC
  s.homepage     = "https://github.com/ekreative/react-native-braintree"
  # brief license entry:
  s.license      = "MIT"
  # optional - use expanded license entry instead:
  # s.license    = { :type => "MIT", :file => "LICENSE" }
  s.authors      = "Ekreative"
  s.platforms    = { :ios => "9.0" }
  s.source       = { :git => "https://github.com/ekreative/react-native-braintree.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,c,cc,cpp,m,mm,swift}"
  s.requires_arc = true

  s.dependency "React"
  s.dependency "Braintree"
  s.dependency "Braintree/DataCollector"
  s.dependency "Braintree/PaymentFlow"
  s.dependency "Braintree/Apple-Pay"

end

