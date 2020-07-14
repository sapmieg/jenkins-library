package cmd

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestTestAbapCommand(t *testing.T) {

	testCmd := TestAbapCommand()

	// only high level testing performed - details are tested in step generation procudure
	assert.Equal(t, "testAbap", testCmd.Use, "command name incorrect")

}
