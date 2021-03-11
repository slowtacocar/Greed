public class KwonSchirmerGeorgeFangGreedStrategy extends GreedStrategy {
    public int choose(GreedOption[] options, int[] dice, int bank) {
		/*for (int i=0; i<dice.length;i++) {
			System.out.print(dice[i] + " ");
		}
		System.out.println("");
		for (int i=0;i<options.length;i++)
			System.out.println(i +": "+ options[i]);
		//if (options[options.length-1].optionType()=1)
			//return */

        if (dice.length>=4) {
            if ((options[0].optionType() == 2 &&
                    !(options[0].toString().contains("one")))
                    || options[0].optionType() == 1 ||
                    findRerollOptionIndex(options) == -1)
                return 0;
            else if (options[0].toString().contains("one"))
                return findRerollOptionIndex(options);
        }
        if (options[0].optionType() == 2 || (dice.length>1 && bank<=250))
            return 0;
        else if (dice.length == 0)
            return findRerollOptionIndex(options);
        else
            return options.length-1;
    }
    private int findRerollOptionIndex(GreedOption[] options) {
        for (int i=0; i<options.length; i++)
            if (options[i].optionType() == 1)
                return i;
        return -1;
    }
    public String playerName() {
        return "Player";
    }
    public String author() {
        return "Fang, George, Kwon, Schirmer";
    }
}

