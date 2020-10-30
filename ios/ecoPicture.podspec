

Pod::Spec.new do |s|



  s.name         = "ecoPicture"
  s.version      = "1.0.0"
  s.summary      = "eco plugin."
  s.description  = <<-DESC
                    eco plugin.
                   DESC

  s.homepage     = "https://eco.app"
  s.license      = "MIT"
  s.author             = { "ViewDesign" => "viewdesign@gmail.com" }
  s.source =  { :path => '.' }
  s.source_files  = "ecoPicture", "**/**/*.{h,m,mm,c}"
  s.exclude_files = "Source/Exclude"
  s.resources = 'ecoPicture/TZImagePickerController/TZImagePickerController.bundle'
  s.platform     = :ios, "8.0"
  s.requires_arc = true

  s.dependency 'WeexSDK'
  s.dependency 'eco'
  s.dependency 'WeexPluginLoader', '~> 0.0.1.9.1'

end
