package com.example.adshield.engine

object ScriptHardening {

    val DOCUMENT_START_JS: String = """
        (function() {
            if (window.__adshield_v2_injected__) return;
            window.__adshield_v2_injected__ = true;

            // 1. Neutralize window.open
            var originalOpen = window.open;
            window.open = function(url, target, features) {
                console.log('[AdShield v2] Blocked popup window.open call:', url);
                return null;
            };

            // 2. Prevent click hijacking & popup event listeners on window/document
            var blockEvents = ['click', 'mousedown', 'mouseup', 'touchstart', 'touchend', 'pointerdown'];
            var originalAddEventListener = EventTarget.prototype.addEventListener;
            EventTarget.prototype.addEventListener = function(type, listener, options) {
                if (this === window || this === document || this === document.body) {
                    if (blockEvents.indexOf(type) !== -1) {
                        var fnStr = listener ? listener.toString() : '';
                        if (fnStr.indexOf('open(') !== -1 || fnStr.indexOf('popunder') !== -1 || fnStr.indexOf('location.href') !== -1) {
                            console.log('[AdShield v2] Neutralized ad click listener on target:', type);
                            return;
                        }
                    }
                }
                return originalAddEventListener.apply(this, arguments);
            };

            // 3. Wrap sendBeacon & WebSocket to log/restrict background ad beacons
            if (navigator.sendBeacon) {
                var originalBeacon = navigator.sendBeacon;
                navigator.sendBeacon = function(url, data) {
                    console.log('[AdShield v2] Intercepted sendBeacon:', url);
                    return true;
                };
            }

            // 4. CSS Injection to strip full-screen click-hijack overlay divs
            function stripOverlays() {
                var elements = document.querySelectorAll('div, a, iframe');
                for (var i = 0; i < elements.length; i++) {
                    var el = elements[i];
                    var style = window.getComputedStyle(el);
                    if (style.position === 'fixed' || style.position === 'absolute') {
                        var zIndex = parseInt(style.zIndex, 10);
                        var opacity = parseFloat(style.opacity);
                        if ((zIndex > 9999 || opacity === 0) && el.tagName !== 'VIDEO') {
                            var rect = el.getBoundingClientRect();
                            if (rect.width >= window.innerWidth * 0.9 && rect.height >= window.innerHeight * 0.9) {
                                console.log('[AdShield v2] Disabled overlay element:', el);
                                el.style.pointerEvents = 'none';
                                el.style.display = 'none';
                            }
                        }
                    }
                }
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', stripOverlays);
            } else {
                stripOverlays();
            }
            setInterval(stripOverlays, 1000);
        })();
    """.trimIndent()
}
