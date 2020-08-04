package cmd

import (
	"bytes"
	"io/ioutil"
	"net/http"
	"testing"

	"github.com/SAP/jenkins-library/pkg/abaputils"
	"github.com/SAP/jenkins-library/pkg/mock"
	"github.com/pkg/errors"

	"github.com/stretchr/testify/assert"
)

// func TestStep(t *testing.T) {
// 	t.Run("Run Step Successful", func(t *testing.T) {

// 		var autils = abaputils.AUtilsMock{}
// 		defer autils.Cleanup()
// 		autils.ReturnedConnectionDetailsHTTP.Password = "password"
// 		autils.ReturnedConnectionDetailsHTTP.User = "user"
// 		autils.ReturnedConnectionDetailsHTTP.URL = "https://example.com"
// 		autils.ReturnedConnectionDetailsHTTP.XCsrfToken = "xcsrftoken"

// 		config := abapEnvironmentPullGitRepoOptions{
// 			CfAPIEndpoint:     "https://api.endpoint.com",
// 			CfOrg:             "testOrg",
// 			CfSpace:           "testSpace",
// 			CfServiceInstance: "testInstance",
// 			CfServiceKeyName:  "testServiceKey",
// 			Username:          "testUser",
// 			Password:          "testPassword",
// 			RepositoryNames:   []string{"testRepo1"},
// 		}

// 		client := &clientMock{
// 			BodyList: []string{
// 				`{"d" : { "status" : "S" } }`,
// 				`{"d" : { "status" : "S" } }`,
// 				`{"d" : { "status" : "S" } }`,
// 			},
// 			Token:      "myToken",
// 			StatusCode: 200,
// 		}

// 		err := runAbapEnvironmentPullGitRepo(&config, nil, &autils, client)
// 		assert.NoError(t, err, "Did not expect error")
// 	})
// }

func TestTriggerPull(t *testing.T) {

	t.Run("Test trigger pull: success case", func(t *testing.T) {

		receivedURI := "example.com/Entity"
		uriExpected := receivedURI + "?$expand=to_Execution_log,to_Transport_log"
		tokenExpected := "myToken"
		returnedBody := `{"d" : { "__metadata" : { "uri" : "` + receivedURI + `" } } }`

		con := abaputils.ConnectionDetailsHTTP{
			User:     "MY_USER",
			Password: "MY_PW",
			URL:      "https://api.endpoint.com/Entity/",
		}

		header := http.Header{}
		header.Set("X-Csrf-Token", tokenExpected)

		requestMockHead := mock.RequestMock{
			URL:    con.URL,
			Method: "HEAD",
			Body:   bytes.NewBuffer([]byte("")),
			Response: http.Response{
				StatusCode: 200,
				Header:     header,
				Body:       ioutil.NopCloser(bytes.NewReader([]byte(""))),
			},
			Err: nil,
		}

		requestMockPost := mock.RequestMock{
			URL:    con.URL,
			Method: "POST",
			Body:   bytes.NewBuffer([]byte(`{"sc_name":"testRepo1"}`)),
			Response: http.Response{
				StatusCode: 200,
				Header:     header,
				Body:       ioutil.NopCloser(bytes.NewReader([]byte(returnedBody))),
			},
			Err: nil,
		}

		requestMockList := make([]mock.RequestMock, 0)
		requestMockList = append(requestMockList, requestMockHead)
		requestMockList = append(requestMockList, requestMockPost)

		client := &mock.ClientMock{
			MockedRequests: requestMockList,
		}

		config := abapEnvironmentPullGitRepoOptions{
			CfAPIEndpoint:     "https://api.endpoint.com",
			CfOrg:             "testOrg",
			CfSpace:           "testSpace",
			CfServiceInstance: "testInstance",
			CfServiceKeyName:  "testServiceKey",
			Username:          "testUser",
			Password:          "testPassword",
			RepositoryNames:   []string{"testRepo1", "testRepo2"},
		}

		entityConnection, err := triggerPull(config.RepositoryNames[0], con, client)
		assert.Nil(t, err)
		assert.Equal(t, uriExpected, entityConnection.URL)
		assert.Equal(t, tokenExpected, entityConnection.XCsrfToken)
	})

	t.Run("Test trigger pull: ABAP Error", func(t *testing.T) {

		errorMessage := "ABAP Error Message"
		errorCode := "ERROR/001"
		HTTPErrorMessage := "HTTP Error Message"
		combinedErrorMessage := "HTTP Error Message: ERROR/001 - ABAP Error Message"

		con := abaputils.ConnectionDetailsHTTP{
			User:     "MY_USER",
			Password: "MY_PW",
			URL:      "https://api.endpoint.com/Entity/",
		}

		requestMockHead := mock.RequestMock{
			URL:    con.URL,
			Method: "HEAD",
			Body:   bytes.NewBuffer([]byte("")),
			Response: http.Response{
				StatusCode: 400,
				Status:     HTTPErrorMessage,
				Header:     nil,
				Body:       ioutil.NopCloser(bytes.NewReader([]byte(`{"error" : { "code" : "` + errorCode + `", "message" : { "lang" : "en", "value" : "` + errorMessage + `" } } }`))),
			},
			Err: errors.New(HTTPErrorMessage),
		}

		config := abapEnvironmentPullGitRepoOptions{
			CfAPIEndpoint:     "https://api.endpoint.com",
			CfOrg:             "testOrg",
			CfSpace:           "testSpace",
			CfServiceInstance: "testInstance",
			CfServiceKeyName:  "testServiceKey",
			Username:          "testUser",
			Password:          "testPassword",
			RepositoryNames:   []string{"testRepo1", "testRepo2"},
		}

		requestMockList := make([]mock.RequestMock, 0)
		requestMockList = append(requestMockList, requestMockHead)

		client := &mock.ClientMock{
			MockedRequests: requestMockList,
		}

		_, err := triggerPull(config.RepositoryNames[0], con, client)
		assert.Equal(t, combinedErrorMessage, err.Error(), "Different error message expected")
	})

}

func TestPollEntity(t *testing.T) {

	// t.Run("Test poll entity: success case", func(t *testing.T) {

	// 	client := &clientMock{
	// 		BodyList: []string{
	// 			`{"d" : { "status" : "S" } }`,
	// 			`{"d" : { "status" : "R" } }`,
	// 		},
	// 		Token:      "myToken",
	// 		StatusCode: 200,
	// 	}
	// 	config := abapEnvironmentPullGitRepoOptions{
	// 		CfAPIEndpoint:     "https://api.endpoint.com",
	// 		CfOrg:             "testOrg",
	// 		CfSpace:           "testSpace",
	// 		CfServiceInstance: "testInstance",
	// 		CfServiceKeyName:  "testServiceKey",
	// 		Username:          "testUser",
	// 		Password:          "testPassword",
	// 		RepositoryNames:   []string{"testRepo1", "testRepo2"},
	// 	}

	// 	con := abaputils.ConnectionDetailsHTTP{
	// 		User:       "MY_USER",
	// 		Password:   "MY_PW",
	// 		URL:        "https://api.endpoint.com/Entity/",
	// 		XCsrfToken: "MY_TOKEN",
	// 	}
	// 	status, _ := pollEntity(config.RepositoryNames[0], con, client, 0)
	// 	assert.Equal(t, "S", status)
	// })

	// t.Run("Test poll entity: error case", func(t *testing.T) {

	// 	client := &clientMock{
	// 		BodyList: []string{
	// 			`{"d" : { "status" : "E" } }`,
	// 			`{"d" : { "status" : "R" } }`,
	// 		},
	// 		Token:      "myToken",
	// 		StatusCode: 200,
	// 	}
	// 	config := abapEnvironmentPullGitRepoOptions{
	// 		CfAPIEndpoint:     "https://api.endpoint.com",
	// 		CfOrg:             "testOrg",
	// 		CfSpace:           "testSpace",
	// 		CfServiceInstance: "testInstance",
	// 		CfServiceKeyName:  "testServiceKey",
	// 		Username:          "testUser",
	// 		Password:          "testPassword",
	// 		RepositoryNames:   []string{"testRepo1", "testRepo2"},
	// 	}

	// 	con := abaputils.ConnectionDetailsHTTP{
	// 		User:       "MY_USER",
	// 		Password:   "MY_PW",
	// 		URL:        "https://api.endpoint.com/Entity/",
	// 		XCsrfToken: "MY_TOKEN",
	// 	}
	// 	status, _ := pollEntity(config.RepositoryNames[0], con, client, 0)
	// 	assert.Equal(t, "E", status)
	// })

}

func TestGetAbapCommunicationArrangementInfo(t *testing.T) {

	t.Run("Test cf cli command: success case", func(t *testing.T) {

		config := abaputils.AbapEnvironmentOptions{
			CfAPIEndpoint:     "https://api.endpoint.com",
			CfOrg:             "testOrg",
			CfSpace:           "testSpace",
			CfServiceInstance: "testInstance",
			CfServiceKeyName:  "testServiceKey",
			Username:          "testUser",
			Password:          "testPassword",
		}

		options := abaputils.AbapEnvironmentPullGitRepoOptions{
			AbapEnvOptions: config,
		}

		execRunner := &mock.ExecMockRunner{}
		var autils = abaputils.AbapUtils{
			Exec: execRunner,
		}

		autils.GetAbapCommunicationArrangementInfo(options.AbapEnvOptions, "/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY/Pull")
		assert.Equal(t, "cf", execRunner.Calls[0].Exec, "Wrong command")
		assert.Equal(t, []string{"login", "-a", "https://api.endpoint.com", "-o", "testOrg", "-s", "testSpace", "-u", "testUser", "-p", "testPassword"}, execRunner.Calls[0].Params, "Wrong parameters")
		//assert.Equal(t, []string{"api", "https://api.endpoint.com"}, execRunner.Calls[0].Params, "Wrong parameters")

	})

	t.Run("Test cf cli command: params missing", func(t *testing.T) {

		config := abaputils.AbapEnvironmentOptions{
			//CfServiceKeyName:  "testServiceKey", this parameter will be missing
			CfAPIEndpoint:     "https://api.endpoint.com",
			CfOrg:             "testOrg",
			CfSpace:           "testSpace",
			CfServiceInstance: "testInstance",
			Username:          "testUser",
			Password:          "testPassword",
		}

		options := abaputils.AbapEnvironmentPullGitRepoOptions{
			AbapEnvOptions: config,
		}

		execRunner := &mock.ExecMockRunner{}
		var autils = abaputils.AbapUtils{
			Exec: execRunner,
		}
		var _, err = autils.GetAbapCommunicationArrangementInfo(options.AbapEnvOptions, "/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY/Pull")
		assert.Equal(t, "Parameters missing. Please provide EITHER the Host of the ABAP server OR the Cloud Foundry ApiEndpoint, Organization, Space, Service Instance and a corresponding Service Key for the Communication Scenario SAP_COM_0510", err.Error(), "Different error message expected")
	})

	t.Run("Test cf cli command: params missing", func(t *testing.T) {

		config := abaputils.AbapEnvironmentOptions{
			Username: "testUser",
			Password: "testPassword",
		}

		options := abaputils.AbapEnvironmentPullGitRepoOptions{
			AbapEnvOptions: config,
		}

		execRunner := &mock.ExecMockRunner{}
		var autils = abaputils.AbapUtils{
			Exec: execRunner,
		}
		var _, err = autils.GetAbapCommunicationArrangementInfo(options.AbapEnvOptions, "/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY/Pull")
		assert.Equal(t, "Parameters missing. Please provide EITHER the Host of the ABAP server OR the Cloud Foundry ApiEndpoint, Organization, Space, Service Instance and a corresponding Service Key for the Communication Scenario SAP_COM_0510", err.Error(), "Different error message expected")
	})

}

func TestTimeConverter(t *testing.T) {
	t.Run("Test example time", func(t *testing.T) {
		inputDate := "/Date(1585576809000+0000)/"
		expectedDate := "2020-03-30 14:00:09 +0000 UTC"
		result := convertTime(inputDate)
		assert.Equal(t, expectedDate, result.String(), "Dates do not match after conversion")
	})
	t.Run("Test Unix time", func(t *testing.T) {
		inputDate := "/Date(0000000000000+0000)/"
		expectedDate := "1970-01-01 00:00:00 +0000 UTC"
		result := convertTime(inputDate)
		assert.Equal(t, expectedDate, result.String(), "Dates do not match after conversion")
	})
	t.Run("Test unexpected format", func(t *testing.T) {
		inputDate := "/Date(0012300000001+0000)/"
		expectedDate := "1970-01-01 00:00:00 +0000 UTC"
		result := convertTime(inputDate)
		assert.Equal(t, expectedDate, result.String(), "Dates do not match after conversion")
	})
}
