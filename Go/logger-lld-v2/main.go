package main

import (
	"fmt"
	"os"

	"github.com/sahilrana27582/logger-lld-v2/logger"
)

func main() {
	consoleLog, err := logger.New(logger.INFO, logger.CONSOLE)
	if err != nil {
		fmt.Println("could not start console logger:", err)
		os.Exit(1)
	}
	defer consoleLog.Close()

	consoleLog.Debug("cache warmed") // skipped: DEBUG is below INFO
	consoleLog.Info("server started")
	consoleLog.Warn("disk 80% full")
	consoleLog.Error("payment service down")

	fileLog, err := logger.New(logger.WARN, logger.FILE)
	if err != nil {
		fmt.Println("could not start file logger:", err)
		os.Exit(1)
	}
	defer fileLog.Close()

	fileLog.Info("user logged in") // skipped: INFO is below WARN
	fileLog.Warn("retrying payment")
	fileLog.Error("payment failed after 3 retries")
}
