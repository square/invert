// This prevents the webpack output from clearing/cleaning the directory first
// Without this the other files are deleted.
config.output = {
  ...config.output,
  clean: false
};