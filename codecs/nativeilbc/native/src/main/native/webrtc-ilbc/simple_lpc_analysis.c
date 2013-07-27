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

 WebRtcIlbcfix_SimpleLpcAnalysis.c

******************************************************************/

#include "defines.h"
#include "window32_w32.h"
#include "bw_expand.h"
#include "poly_to_lsf.h"
#include "constants.h"
#include <stdlib.h>
#include <stdio.h>
/*----------------------------------------------------------------*
 *  lpc analysis (subrutine to LPCencode)
 *---------------------------------------------------------------*/

void WebRtcIlbcfix_SimpleLpcAnalysis(
    int16_t *lsf,   /* (o) lsf coefficients */
    int16_t *data,   /* (i) new block of speech */
    int16_t *lpcBuffer /* (i/o) the encoder state structure */
                                     ) {
  int scale;
  int16_t is;
  int16_t stability;
  /* Stack based */
  int16_t A[11];
  int32_t R[11];

  int16_t windowedData[240];
  int16_t rc[10];

  is=140;
  WEBRTC_SPL_MEMCPY_W16(lpcBuffer+is,data,160);

  /* No lookahead, last window is asymmetric */

    is = 60;

    /* Hanning table WebRtcIlbcfix_kLpcAsymWin[] is in Q15-domain so the output is right-shifted 15 */
    WebRtcSpl_ElementwiseVectorMult(windowedData, lpcBuffer+is, WebRtcIlbcfix_kLpcAsymWin, 240, 15);

    /* Compute autocorrelation */
    WebRtcSpl_AutoCorrelation(windowedData, 240, 10, R, &scale);

    /* Window autocorrelation vector */
    WebRtcIlbcfix_Window32W32(R, R, WebRtcIlbcfix_kLpcLagWin, 11 );

    /* Calculate the A coefficients from the Autocorrelation using Levinson Durbin algorithm */
    stability=WebRtcSpl_LevinsonDurbin(R, A, rc, 10);

    /*
       Set the filter to {1.0, 0.0, 0.0,...} if filter from Levinson Durbin algorithm is unstable
       This should basically never happen...
    */
    if (stability!=1) {
      A[0]=4096;
      WebRtcSpl_MemSetW16(&A[1], 0, 10);
    }

    /* Bandwidth expand the filter coefficients */
    WebRtcIlbcfix_BwExpand(A, A, (int16_t*)WebRtcIlbcfix_kLpcChirpSyntDenum, 11);

    /* Convert from A to LSF representation */
    WebRtcIlbcfix_Poly2Lsf(lsf, A);

  is=LPC_LOOKBACK+BLOCKL_MAX-160;
  WEBRTC_SPL_MEMCPY_W16(lpcBuffer,
                        lpcBuffer+LPC_LOOKBACK+BLOCKL_MAX-is, is);

  return;
}
