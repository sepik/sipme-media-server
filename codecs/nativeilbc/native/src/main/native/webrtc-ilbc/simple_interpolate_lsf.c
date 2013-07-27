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

 WebRtcIlbcfix_SimpleInterpolateLsf.c

******************************************************************/

#include "defines.h"
#include "lsf_interpolate_to_poly_enc.h"
#include "bw_expand.h"
#include "constants.h"

/*----------------------------------------------------------------*
 *  lsf interpolator (subrutine to LPCencode)
 *---------------------------------------------------------------*/

void WebRtcIlbcfix_SimpleInterpolateLsf(
    int16_t *syntdenum, /* (o) the synthesis filter denominator
                                   resulting from the quantized
                                   interpolated lsf Q12 */
    int16_t *weightdenum, /* (o) the weighting filter denominator
                                   resulting from the unquantized
                                   interpolated lsf Q12 */
    int16_t *lsf,  /* (i) the unquantized lsf coefficients Q13 */
    int16_t *lsfdeq,  /* (i) the dequantized lsf coefficients Q13 */
    int16_t *lsfold,  /* (i) the unquantized lsf coefficients of
                                           the previous signal frame Q13 */
    int16_t *lsfdeqold, /* (i) the dequantized lsf coefficients of the
                                   previous signal frame Q13 */
    int16_t length  /* (i) should equate FILTERORDER */
                                        ) {
  int i, pos, lp_length;

  int16_t *lsf2, *lsfdeq2;
  /* Stack based */
  int16_t lp[LPC_FILTERORDER + 1];

  lsf2 = lsf + length;
  lsfdeq2 = lsfdeq + length;
  lp_length = length + 1;

    pos = 0;
    for (i = 0; i < 4; i++) {

      /* Calculate Analysis/Syntehsis filter from quantized LSF */
      WebRtcIlbcfix_LsfInterpolate2PloyEnc(lp, lsfdeqold, lsfdeq,
                                           WebRtcIlbcfix_kLsfWeight20ms[i],
                                           length);
      WEBRTC_SPL_MEMCPY_W16(syntdenum + pos, lp, lp_length);

      /* Calculate Weighting filter from quantized LSF */
      WebRtcIlbcfix_LsfInterpolate2PloyEnc(lp, lsfold, lsf,
                                           WebRtcIlbcfix_kLsfWeight20ms[i],
                                           length);
      WebRtcIlbcfix_BwExpand(weightdenum+pos, lp,
                             (int16_t*)WebRtcIlbcfix_kLpcChirpWeightDenum,
                             (int16_t)lp_length);

      pos += lp_length;
    }

    /* update memory */

    WEBRTC_SPL_MEMCPY_W16(lsfold, lsf, length);
    WEBRTC_SPL_MEMCPY_W16(lsfdeqold, lsfdeq, length);

  return;
}
