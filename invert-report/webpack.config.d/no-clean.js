// Custom webpack config
// https://kotlinlang.org/docs/js-project-setup.html#custom-webpack-configuration

// This prevents the webpack output from clearing/cleaning the directory first
// This matches the behavior from Kotlin 2.0.0.
config.output = {
  ...config.output,
  clean: false
};
