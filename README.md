# Assembly Compiler Application

Java Based application that compiles assembly code

## Description

This application allows the user to input assembly code via a .nha file

This file will contain assembly code that the program will translate into binary for the computer to process

The New Hack Assembly language follows certain rules to ensure consistency

### New Hack Assembly

We have the following commands we can issue

#### ldr

This command loads data into memory of our selection, it allows to either load constant or load data from other memory slots

ldr A, $21

This will load 21 into memory slot A with the $ meaning a numeric constant

ldr D, (A)

This will load whatever is in A into D. Notice the parenthesis, these mean we are accessing the data inside A and not loading any data into A

#### str

This command stores data into memory slot A with a parameter including the said data we want to store

str (A), D

This will store data from D into A

#### jmp

This command has many alternatives that we can use to signify our program flow

jmp

Jumps to the start of the program with no condition

jgt D

Jumps if D is greater than 0

jeq A

Jumps if A is equal to 0

jge (A)

Jumps if A is greater or equal to 0

jlt D

Jumps if D is less than 0

jle (A)

Jumps if A is less than or equal to 0

jne D

Jumps if D does not equal to 0

## Getting Started

### Dependencies

* None

### Installing

* Download as a ZIP file or clone the repository
* Open the project in your IDE
* Insert your desired commands in a .nha file

### Executing program

* Simply run the project in your IDE

## Authors

Contributors names and contact info

Nikita Kramtsaninov

## Version History

* 0.1
    * Initial Release
