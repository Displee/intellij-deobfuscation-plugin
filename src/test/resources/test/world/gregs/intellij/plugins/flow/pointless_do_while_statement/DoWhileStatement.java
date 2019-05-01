class NestedElse {

    private int first = 1;
    private int second = 2;
    private int third = 3;

    public void simpleNest() {
        do {
            if (first != 1) {
                break;
            }
        } while (false);
    }

    public void followedByBlock() {
        do {
            if (first != 1) {
                break;
            }
            second = 2;
        } while (false);
        if (first != second) {
            third = 3;
        }
    }

    public void ignoreBreak() {
        do {
            first = 2;
            break;
        } while (false);
    }

    public void named() {
        while_89_:
        do {
            if (first == 1) {
                break while_89_;
            }
            first = 2;
            break;
        } while (false);
    }

}