/*
 *  Copyright (c) 2011 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

/******************************************************************

 iLBC Speech Coder ANSI-C Source Code

 WebRtcIlbcfix_StateSearch.c

******************************************************************/

#include "defines.h"
#include "constants.h"
#include "abs_quant.h"
#include <stdio.h>

/*----------------------------------------------------------------*
 *  encoding of start state
 *---------------------------------------------------------------*/

int16_t WebRtcIlbcfix_StateSearch(
    int16_t state_first,
    int16_t *residual,   /* (i) target residual vector */
    int16_t *syntDenum,  /* (i) lpc synthesis filter */
    int16_t *weightDenum,  /* (i) weighting filter denuminator */
    int16_t *idx_vec
                               ) {
  int16_t k, index;
  int16_t maxVal;
  int16_t scale, shift;
  int32_t maxValsq;
  int16_t scaleRes;
  int16_t max;
  int16_t result;
  int i;
  /* Stack based */
  int16_t numerator[1+LPC_FILTERORDER];
  int16_t residualLongVec[2*STATE_SHORT_LEN_30MS+LPC_FILTERORDER];
  int16_t sampleMa[2*STATE_SHORT_LEN_30MS];
  int16_t *residualLong = &residualLongVec[LPC_FILTERORDER];
  int16_t *sampleAr = residualLong;

  /* Scale to maximum 12 bits to avoid saturation in circular convolution filter */
  max = WebRtcSpl_MaxAbsValueW16(residual, 57);
  scaleRes = WebRtcSpl_GetSizeInBits(max)-12;
  scaleRes = WEBRTC_SPL_MAX(0, scaleRes);
  /* Set up the filter coefficients for the circular convolution */
  for (i=0; i<LPC_FILTERORDER+1; i++) {
    numerator[i] = (syntDenum[LPC_FILTERORDER-i]>>scaleRes);
  }

  /* Copy the residual to a temporary buffer that we can filter
   * and set the remaining samples to zero.
   */
  WEBRTC_SPL_MEMCPY_W16(residualLong, residual, 57);
  WebRtcSpl_MemSetW16(residualLong + 57, 0, 57);

  /* Run the Zero-Pole filter (Ciurcular convolution) */
  WebRtcSpl_MemSetW16(residualLongVec, 0, LPC_FILTERORDER);
  WebRtcSpl_FilterMAFastQ12(
      residualLong, sampleMa,
      numerator, LPC_FILTERORDER+1, (int16_t)(57 + LPC_FILTERORDER));
  WebRtcSpl_MemSetW16(&sampleMa[57 + LPC_FILTERORDER], 0, 57 - LPC_FILTERORDER);

  WebRtcSpl_FilterARFastQ12(
      sampleMa, sampleAr,
      syntDenum, LPC_FILTERORDER+1, (int16_t)(2*57));

  for(k=0;k<57;k++){
    sampleAr[k] += sampleAr[k+57];
  }

  /* Find maximum absolute value in the vector */
  maxVal=WebRtcSpl_MaxAbsValueW16(sampleAr, 57);

  /* Find the best index */

  if ((((int32_t)maxVal)<<scaleRes)<23170) {
    maxValsq=((int32_t)maxVal*maxVal)<<(2+2*scaleRes);
  } else {
    maxValsq=(int32_t)WEBRTC_SPL_WORD32_MAX;
  }

  index=0;
  for (i=0;i<63;i++) {

    if (maxValsq>=WebRtcIlbcfix_kChooseFrgQuant[i]) {
      index=i+1;
    } else {
      i=63;
    }
  }
  result=index;

  /* Rescale the vector before quantization */
  scale=WebRtcIlbcfix_kScale[index];

  if (index<27) { /* scale table is in Q16, fout[] is in Q(-1) and we want the result to be in Q11 */
    shift=4;
  } else { /* scale table is in Q21, fout[] is in Q(-1) and we want the result to be in Q11 */
    shift=9;
  }

  /* Set up vectors for AbsQuant and rescale it with the scale factor */
  WebRtcSpl_ScaleVectorWithSat(sampleAr, sampleAr, scale,
                              57, (int16_t)(shift-scaleRes));

  /* Quantize the values in fout[] */
  WebRtcIlbcfix_AbsQuant(state_first, sampleAr, weightDenum,idx_vec);

  return result;
}
