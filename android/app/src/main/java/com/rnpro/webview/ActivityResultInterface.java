package com.rnpro.webview;

import android.content.Intent;

/**
 * Created by cyy
 * on 18-3-22.
 */

interface ActivityResultInterface {
    void callback(int requestCode, int resultCode, Intent data);
}
