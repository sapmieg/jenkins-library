// +build !release

package mock

import (
	"io"
	"io/ioutil"
	"net/http"
	"time"

	piperhttp "github.com/SAP/jenkins-library/pkg/http"
	"github.com/SAP/jenkins-library/pkg/log"
	"github.com/sirupsen/logrus"
)

// ClientMock  for testing Client
type ClientMock struct {
	maxRequestDuration       time.Duration
	transportTimeout         time.Duration
	username                 string
	password                 string
	token                    string
	logger                   *logrus.Entry
	cookieJar                http.CookieJar
	doLogRequestBodyOnDebug  bool
	doLogResponseBodyOnDebug bool
	MockedRequests           []RequestMock
}

// RequestMock - provide method, body and response or error
type RequestMock struct {
	URL      string
	Method   string
	Body     io.Reader
	Response http.Response
	Err      error
}

// SendRequest for ClientMock
func (c *ClientMock) SendRequest(method, url string, body io.Reader, header http.Header, cookies []*http.Cookie) (*http.Response, error) {

	// TODO Order of incoming requests
	for _, request := range c.MockedRequests {
		if url == request.URL && method == request.Method {
			expectedBody, _ := ioutil.ReadAll(request.Body)
			receivedBody, _ := ioutil.ReadAll(body)
			expectedString := string(expectedBody)
			receivedString := string(receivedBody)
			if expectedString == receivedString {
				return &request.Response, request.Err
			}
		}
	}
	return &http.Response{}, nil
}

// var body []byte
// if c.Body != "" {
// 	body = []byte(c.Body)
// } else {
// 	bodyString := c.BodyList[len(c.BodyList)-1]
// 	c.BodyList = c.BodyList[:len(c.BodyList)-1]
// 	body = []byte(bodyString)
// }
// header := http.Header{}
// header.Set("X-Csrf-Token", c.Token)

// SetOptions for Client Mock
func (c *ClientMock) SetOptions(options piperhttp.ClientOptions) {
	c.doLogRequestBodyOnDebug = options.DoLogRequestBodyOnDebug
	c.doLogResponseBodyOnDebug = options.DoLogResponseBodyOnDebug
	c.transportTimeout = options.TransportTimeout
	c.maxRequestDuration = options.MaxRequestDuration
	c.username = options.Username
	c.password = options.Password
	c.token = options.Token

	if options.Logger != nil {
		c.logger = options.Logger
	} else {
		c.logger = log.Entry().WithField("package", "SAP/jenkins-library/pkg/http")
	}
	c.cookieJar = options.CookieJar
}
