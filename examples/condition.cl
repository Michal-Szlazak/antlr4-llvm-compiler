
if(true) {
    write "if (true)";
};

if(false) {
    write "if (false)";
};

if(true OR false) {
    write "if (true OR false)";
};

bool x;
bool y;

x = true;
y = false;

if(x XOR y) {
    write "x XOR y (true XOR false)";
};

if(x AND y) {
    write "x AND y (true AND false)";
};

if(NEG y) {
    write "NEG y (NEG false)";
};