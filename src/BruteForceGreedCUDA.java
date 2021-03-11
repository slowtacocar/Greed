import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.*;

import static jcuda.driver.JCudaDriver.*;

public class BruteForceGreedCUDA extends GreedStrategy {
    private static final CUfunction function = new CUfunction();

    static {
        JCudaDriver.setExceptionsEnabled(true);
        cuInit(0);
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);
        CUmodule module = new CUmodule();
        cuModuleLoad(module, "C:\\Users\\bobby\\IdeaProjects\\Greed\\src\\BruteForceGreed.cubin");
        cuModuleGetFunction(function, module, "chooseOne");
    }

    public static void main(String[] args) {
        SeededGreedGame.ComputerGreedCompetition(new BruteForceGreedCUDA(), new BruteForceGreed()
                , 10, false, 3467091);
    }

    @Override
    public int choose(GreedOption[] options, int[] dice, int bank) {
        CUdeviceptr deviceDice = new CUdeviceptr();
        cuMemAlloc(deviceDice, (long) dice.length * Sizeof.INT);
        cuMemcpyHtoD(deviceDice, Pointer.to(dice), (long) dice.length * Sizeof.INT);
        CUdeviceptr deviceResult = new CUdeviceptr();
        cuMemAlloc(deviceResult, 2 * Sizeof.INT);
        int rolled = 1;
        if (options[options.length - 1].optionType() == GreedOption.ENDTURN) {
            rolled = 0;
        }
        Pointer kernelParameters = Pointer.to(Pointer.to(deviceDice), Pointer.to(new int[]{dice.length}),
                Pointer.to(new int[]{bank}), Pointer.to(new int[]{0}), Pointer.to(new int[]{rolled}),
                Pointer.to(deviceResult));
        cuLaunchKernel(function, 1, 1, 1, 1, 1, 1, 0, null, kernelParameters, null);
        cuCtxSynchronize();
        int[] result = new int[2];
        cuMemcpyDtoH(Pointer.to(result), deviceResult, 2 * Sizeof.INT);
        cuMemFree(deviceDice);
        cuMemFree(deviceResult);
        return result[1];
    }
}
