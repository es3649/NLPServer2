let vue = new Vue({
  el: "#root",
  data: {
    password: "",
    message: "",
    name: "",
    number: "",
    response: "",
    loading: false
  },
  methods: {
    // makes an API call to the given handle. Using a variable here makes calle to the granular
    // or basic API use the same code.
    async APICall(handle) {
      // display a loading message
      this.loading = true;

      // construct a request based on the structure of the request class in the java code
      req = {

      }

      console.log(req)

      try {

        const headers = {
          Password: this.password
        }

        console.log(headers.Password)

        // make the API call
        const resp = await axios.post(handle, req, {headers: headers})

        console.log(resp)

      } catch (error) {

        // awkward, log the error
        console.log(error)

      } finally {

        // clear loading status
        this.loading = false;
      }
    },
    async analyze() {
      await this.APICall("/analyze")
    },
    async granular() {
      await this.APICall("/granularAnalysis")
    }
  }
})
