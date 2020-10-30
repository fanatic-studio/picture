//
//  ecoPicture.m
//  Pods
//
//  Created by 高一 on 2019/3/4.
//

#import "ecoPicture.h"
#import "ecoPictureBridge.h"
#import "WeexInitManager.h"

WEEX_PLUGIN_INIT(ecoPicture)
@implementation ecoPicture

+ (instancetype) sharedManager {
    static dispatch_once_t onceToken;
    static ecoPicture *instance;
    dispatch_once(&onceToken, ^{
        instance = [[ecoPicture alloc] init];
    });
    return instance;
}

- (void) setJSCallModule:(JSCallCommon *)callCommon webView:(WKWebView*)webView
{
    [callCommon setJSCallAssign:webView name:@"ecoPicture" bridge:[[ecoPictureBridge alloc] init]];
}

@end
