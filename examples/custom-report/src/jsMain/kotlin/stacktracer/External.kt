package stacktracer// Use external functions to call JS functions defined in global scope

external fun externalLoadStacktracerMapping(url: String, callback: (json: String) -> Unit)
