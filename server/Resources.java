package server;

enum ERRORS {
    WRONG_PATH_TO_PROPERTY("Wrong path to property"),
    NO_SUCH_KEY("No such key"),
    INCORRECT_COMMAND("Incorrect command"),
    MALFORMED_REQUEST("Malformed request"),
    WRONG_PATH_TO_DB("Wrong path for db file"),
    DB_FILE_IO_ERROR("Error while trying to work with DB file");

    final String text;
    ERRORS(String text) {
        this.text = text;
    }

}
