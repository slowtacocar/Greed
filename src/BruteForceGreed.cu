__device__ int scs[][8] = { {6, 5000, 6, 6, 6, 6, 6, 6}, {6, 5000, 5, 5, 5, 5, 5, 5}, {6, 5000, 4, 4, 4, 4, 4, 4}, {6, 5000, 3, 3, 3, 3, 3, 3}, {6, 5000, 2, 2, 2, 2, 2, 2}, {6, 5000, 1, 1, 1, 1, 1, 1}, {6, 2000, 4, 4, 5, 5, 6, 6}, {6, 2000, 3, 3, 4, 4, 5, 5}, {6, 2000, 2, 2, 3, 3, 4, 4}, {6, 2000, 1, 1, 2, 2, 3, 3}, {6, 1000, 1, 2, 3, 4, 5, 6}, {4, 1000, 1, 1, 1, 1}, {3, 600, 6, 6, 6}, {3, 500, 5, 5, 5}, {3, 400, 4, 4, 4}, {3, 300, 3, 3, 3}, {3, 200, 2, 2, 2}, {1, 100, 1}, {1, 50, 5} };

__device__ void getAllCombinations(int dice, int** result, int start)
{
	if (dice > 0)
	{
		int size = 1;
		for (int i = 0; i < dice - 1; i++)
		{
			size *= 6;
		}
		for (int i = 0; i < 6; i++)
		{
		    getAllCombinations(dice - 1, result, start + i * size);
			for (int j = 0; j < size; j++)
			{
				result[start + i * size + j][dice - 1] = i + 1;
			}
		}
	}
}

__device__ void quicksort(int array[], int start, int end)
{
	if (start < end)
	{
		int pivot = array[end];
		int i = start - 1;
		for (int j = start; j < end; j++)
		{
			if (array[j] <= pivot)
			{
				i++;
				int swapTemp = array[i];
				array[i] = array[j];
				array[j] = swapTemp;
			}
		}
		int swapTemp = array[i + 1];
		array[i + 1] = array[end];
		array[end] = swapTemp;
		quicksort(array, start, i);
		quicksort(array, i + 2, end);
	}
}

__global__ void choose(int** combinations, int numDice, int bank, int depth, int rollDepth, bool rolled, int results[], int start, int tot)
{
	if (tot > start + blockIdx.x * blockDim.x + threadIdx.x)
	{
	    int* dice = combinations[start + blockIdx.x * blockDim.x + threadIdx.x];
        int* result = results + 2 * (start + blockIdx.x * blockDim.x + threadIdx.x);
        if (depth > 2)
        {
            result[0] = 0;
        }
        else
        {
            int nc = 0;
            bool answers[19] = {};
            for (int i = 0; i < 19; i++)
            {
                int first = 0;
                for (int j = 0; j < numDice; j++)
                {
                    if (dice[j] == scs[i][first + 2])
                    {
                        first++;
                    }
                    else if (first > 0)
                    {
                        break;
                    }
                    if (first >= scs[i][0])
                    {
                        answers[i] = true;
                        nc++;
                        break;
                    }
                }
            }
            int l = 0;
            int** choices = new int* [nc];
            for (int i = 0; i < 19; i++)
            {
                if (answers[i])
                {
                    choices[l] = scs[i];
                    l++;
                }
            }
            result[0] = bank;
            result[1] = nc + 1;
            if (!rolled)
            {
                int length = numDice;
                if (length == 0)
                {
                    length = 6;
                }
                int size = 1;
                for (int i = 0; i < length; i++)
                {
                    size *= 6;
                }
                int** combos = new int* [size];
                for (int i = 0; i < size; i++)
                {
                    combos[i] = new int[length];
                }
                getAllCombinations(length, combos, 0);
                int** analyzedCombos = new int* [size];
                int* analyzedComboScores = new int[size];
                int nextIndex = 0;
                int sum = 0;
                for (int i = 0; i < size; i++)
                {
                    quicksort(combos[i], 0, length - 1);
                    int index = -1;
                    for (int j = 0; j < nextIndex; j++)
                    {
                        bool equal = true;
                        for (int k = 0; k < length; k++)
                        {
                            if (analyzedCombos[j][k] != combos[i][k])
                            {
                                equal = false;
                                break;
                            }
                        }
                        if (equal)
                        {
                            index = j;
                            break;
                        }
                    }
                    if (index > -1)
                    {
                        analyzedComboScores[index]++;
                    }
                    else
                    {
                        analyzedCombos[nextIndex] = combos[i];
                        analyzedComboScores[nextIndex] = 1;
                        nextIndex++;
                    }
                }
                if (rollDepth == -1)
                {
                    int* score = new int[nextIndex * 2];
                    choose<<<nextIndex / 256, 256>>>(analyzedCombos, length, bank, depth + 1, rollDepth + 1, true, score, 0, nextIndex);
                    cudaDeviceSynchronize();
                    for (int i = 0; i < nextIndex; i++)
                    {
                        sum += score[i * 2] * analyzedComboScores[i];
                    }
                    delete score;
}
                else
                {
                    int* score = new int[2];
                    for (int i = 0; i < nextIndex; i++)
                    {
                        choose<<<1, 1>>>(analyzedCombos, length, bank, depth + 1, rollDepth + 1, true, score, i, 1);
                        cudaDeviceSynchronize();
                        sum += score[0] * analyzedComboScores[i];
                    }
                    delete score;
                }
                if (sum / size > result[0])
                {
                    result[0] = sum / size;
                    result[1] = nc;
                }
                delete analyzedComboScores;
                delete analyzedCombos;
                for (int i = 0; i < size; i++)
                {
                    delete combos[i];
                }
                delete combos;
            }
            if (!rolled || nc > 1)
            {
                for (int i = 0; i < nc; i++)
                {
                    int* score = new int[2];
                    int length = numDice - choices[i][0];
                    int* newCombo = new int[length];
                    int index = 0;
                    for (int j = 0; j < numDice; j++)
                    {
                        if (index < choices[i][0] && dice[j] == choices[i][index + 2])
                        {
                            index++;
                        }
                        else
                        {
                            newCombo[j - index] = dice[j];
                        }
                    }
                    int** newCombinations = new int* [] {newCombo};
                    choose<<<1, 1>>>(newCombinations, length, bank + choices[i][1], depth + 1, rollDepth, false, score, 0, 1);
                    cudaDeviceSynchronize();
                    if (choices[i][1] + score[0] > result[0])
                    {
                        result[0] = choices[i][1] + score[0];
                        result[1] = i;
                    }
                    delete newCombinations;
                    delete newCombo;
                    delete score;
                }
            }
            else if (nc == 0)
            {
                result[0] = 0;
            }
            else
            {
                int* score = new int[2];
                int length = numDice - choices[0][0];
                int* newCombo = new int[length];
                int index = 0;
                for (int j = 0; j < numDice; j++)
                {
                    if (index < choices[0][0] && dice[j] == choices[0][index + 2])
                    {
                        index++;
                    }
                    else
                    {
                        newCombo[j - index] = dice[j];
                    }
                }
                int** newCombinations = new int* [] {newCombo};
                choose<<<1, 1>>>(newCombinations, length, bank + choices[0][1], depth + 1, rollDepth, false, score, 0, 1);
                cudaDeviceSynchronize();
                result[0] = choices[0][1] + score[0];
                result[1] = 0;
                delete newCombinations;
                delete newCombo;
                delete score;
            }
            delete choices;
        }
    }
}

extern "C"
__global__ void chooseOne(int* dice, int numDice, int bank, int depth, int rolled, int results[])
{
    int** combinations = new int* [] {dice};
    choose<<<1, 1>>>(combinations, numDice, bank, depth, 0, rolled == 1, results, 0, 1);
    cudaDeviceSynchronize();
    delete combinations;
}
