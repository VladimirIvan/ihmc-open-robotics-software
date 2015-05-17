package us.ihmc.darpaRoboticsChallenge.sensors.microphone;

import us.ihmc.simulationconstructionset.gui.BodePlotConstructor;
import us.ihmc.utilities.linearDynamicSystems.BodeUnitsConverter;

/**
 * <p>Description: Detects a distinct sound by searching for a characteristic peak in FFT magnitude data of sound data
 * from the Atlas Chest Webcam microphone around a given frequency</p>
 */
public class DrillDetectionAlgorithmOriginal extends DrillDetectionAlgorithm
{

   /**
    * The relevant frequency band is the range of frequencies that are used for comparison to distinguish a peak.
    * The magnitude data is averaged across this range with the exception of the dominant frequency band magnitudes.
    * <p/>
    * The dominant frequency band is the range of frequencies were the peak in magnitude could show up in. This band must be within the relevant band.
    * <p/>
    * The decibelsDeltaToTripDetection is the minimum difference in magnitude the peak must have over the average magnitude to be considered a significant
    * enough peak to trip detection.
    */

   private static final double decibelsDeltaToTripDetection = -5.8; //dB
   private static final double dominantFrequencyBandLowerBound = 6900; //Hz
   private static final double dominantFrequencyBandUpperBound = 7900; //Hz
   private static final double relevantFrequencyBandLowerBound = 1000; //Hz
   private static final double relevantFrequencyBandUpperBound = 4900; //Hz

   @Override
   public DrillDetectionResult isDrillOn(byte[] audioBytes, int size)
   {
      int nlengthInSamples = size / 2;
      int[] audioData = new int[nlengthInSamples];

      //Little-Endian code
      for (int i = 0; i < nlengthInSamples; i++)
      {
         int LSB = (int) audioBytes[2 * i];     /* First byte is LSB (low order) */
         int MSB = (int) audioBytes[2 * i + 1]; /* Second byte is MSB (high order) */
         audioData[i] = (MSB << 8) | (LSB & 0xFF);
      }

      double[] input = new double[audioData.length];
      double[] time = new double[audioData.length];
      for (int i = 0; i < audioData.length; i++)
      {
         input[i] = (double) audioData[i];
         time[i] = (double) i / getSampleRate();
      }

      double[][] fftData = BodePlotConstructor.computeFreqMagPhase(time, input);
      double[] frequency = fftData[0];
      double[] magnitude = BodeUnitsConverter.convertMagnitudeToDecibels(fftData[1]);

      //Peak detection math
      int dominantFrequencyBandLowerBoundIndex = 0;
      int dominantFrequencyBandUpperBoundIndex = 0;
      int relevantFrequencyBandLowerBoundIndex = 0;
      int relevantFrequencyBandUpperBoundIndex = 0;

      for (int index = 0; index < frequency.length; index++)
      {
         if (frequency[index] <= dominantFrequencyBandLowerBound)
         {
            dominantFrequencyBandLowerBoundIndex = index;
         }

         if (frequency[index] <= dominantFrequencyBandUpperBound)
         {
            dominantFrequencyBandUpperBoundIndex = index;
         }

         if (frequency[index] <= relevantFrequencyBandLowerBound)
         {
            relevantFrequencyBandLowerBoundIndex = index;
         }

         if (frequency[index] <= relevantFrequencyBandUpperBound)
         {
            relevantFrequencyBandUpperBoundIndex = index;
         }
      }

      double dominantBandAverageMag = 0;
      double relevantBandAverageMag = 0;

      int dominantRangeSize = dominantFrequencyBandUpperBoundIndex - dominantFrequencyBandLowerBoundIndex;
      int relevantRangeSize = relevantFrequencyBandUpperBoundIndex - relevantFrequencyBandLowerBoundIndex;

      for (int index = 0; index < relevantRangeSize; index++)
      {
         relevantBandAverageMag += magnitude[relevantFrequencyBandLowerBoundIndex + index];
      }
      relevantBandAverageMag /= relevantRangeSize;

      for (int index = 0; index < dominantRangeSize; index++)
      {
         dominantBandAverageMag += magnitude[dominantFrequencyBandLowerBoundIndex + index];
      }
      dominantBandAverageMag /= dominantRangeSize;

      DrillDetectionResult result = new DrillDetectionResult();
      result.isOn = ((dominantBandAverageMag - relevantBandAverageMag) > decibelsDeltaToTripDetection);
      result.averageValues = new double[] { relevantBandAverageMag, dominantBandAverageMag };
      result.bodeData = getBodeData(time, input);

      return result;
   }
}