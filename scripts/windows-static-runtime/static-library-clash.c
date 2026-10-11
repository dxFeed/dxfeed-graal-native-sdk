/*
 * The application's own zlib and JNU_* functions, which the JDK in the static library of the SDK defines too: the
 * library hides its internal symbols, so the link has no LNK2005.
 */
int deflate(void *stream, int flush) {
    (void)stream;
    return flush;
}

int inflate(void *stream, int flush) {
    (void)stream;
    return flush;
}

int JNU_ThrowByName(void) {
    return 0;
}
