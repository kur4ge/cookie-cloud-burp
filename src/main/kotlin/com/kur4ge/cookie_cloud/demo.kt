package com.kur4ge.cookie_cloud

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi

@Suppress("unused")
class HelloWorld : BurpExtension {
   override fun initialize(api: MontoyaApi?) {
        if (api == null) {
            return
        }

        api.extension().setName("Cookie Cloud")
        api.logging().logToOutput("HelloWorld")
    }
}
