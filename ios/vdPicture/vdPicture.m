//
//  vdPicture.m
//  Pods
//
//  Created by 高一 on 2019/3/4.
//

#import "vdPicture.h"
#import "vdPictureBridge.h"
#import "WeexInitManager.h"

WEEX_PLUGIN_INIT(vdPicture)
@implementation vdPicture

+ (instancetype) sharedManager {
    static dispatch_once_t onceToken;
    static vdPicture *instance;
    dispatch_once(&onceToken, ^{
        instance = [[vdPicture alloc] init];
    });
    return instance;
}

- (void) setJSCallModule:(JSCallCommon *)callCommon webView:(WKWebView*)webView
{
    [callCommon setJSCallAssign:webView name:@"vdPicture" bridge:[[vdPictureBridge alloc] init]];
}

@end
