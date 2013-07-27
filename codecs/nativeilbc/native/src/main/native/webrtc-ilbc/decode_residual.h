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

 WebRtcIlbcfix_DecodeResidual.h

******************************************************************/

#ifndef WEBRTC_MODULES_AUDIO_CODING_CODECS_ILBC_MAIN_SOURCE_DECODE_RESIDUAL_H_
#define WEBRTC_MODULES_AUDIO_CODING_CODECS_ILBC_MAIN_SOURCE_DECODE_RESIDUAL_H_

#include "defines.h"

/*----------------------------------------------------------------*
 *  frame residual decoder function (subrutine to iLBC_decode)
 *---------------------------------------------------------------*/

void WebRtcIlbcfix_DecodeResidual(
    int16_t *memVec,
    int16_t *reverseDecresidual,
    int16_t *idxVec,
    int16_t *cb_index,
    int16_t *gain_index,
    int16_t *decresidual,  /* (o) decoded residual frame */
    int16_t *syntdenum,   /* (i) the decoded synthesis filter
                                  coefficients */
    int16_t state_short_len,
    int16_t nsub,
    int16_t state_first,
    int16_t startIdx,
    int16_t idxForMax
                                  );
#endif

