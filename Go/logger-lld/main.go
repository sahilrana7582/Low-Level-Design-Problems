package main

import "github.com/sahilrana27582/logger-lld/logger"

func main() {
	logger := logger.New("info")

	logger.Info("Logging")
	logger.Debug("Logging")
	logger.Warn("Logging")
	logger.Error("Logging")
}
