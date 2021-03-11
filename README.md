# Greed

A program that plays the Greed dice game

This program contains code written by my Computer Science teacher for a functional Greed game and an interface to write custom strategies which a computer can use to play the game.

I added BruteForceGreed.java - a Greed strategy which recursively evaluates every possible choice and every possible dice roll, calculates the average final score for each choice, and picks the best option. As this is a computationally intensive task, the strategy includes a cache containing all evaluated game states and the best option to choose for each. The program is also multithreaded and has adjustable recursion depth.